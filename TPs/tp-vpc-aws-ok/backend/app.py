from flask import Flask, jsonify, request, render_template
from flask_sqlalchemy import SQLAlchemy
from flask_cors import CORS
from datetime import datetime
import os
import logging

# Configuration du logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__, template_folder='../web-app/templates', static_folder='../web-app/static')
CORS(app, resources={r"/api/*": {"origins": os.environ.get("ALLOWED_ORIGINS", "*")}})

# Configuration de la base de données via variables d'environnement
DB_HOST = os.environ.get("DB_HOST", "postgres")
DB_PORT = os.environ.get("DB_PORT", "5432")
DB_NAME = os.environ.get("DB_NAME", "appdb")
DB_USER = os.environ.get("DB_USER", "appuser")
DB_PASS = os.environ.get("DB_PASS", "changeme")

app.config["SQLALCHEMY_DATABASE_URI"] = (
    f"postgresql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
)
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
app.config["SECRET_KEY"] = os.environ.get("SECRET_KEY", "dev-secret-key-change-in-prod")

db = SQLAlchemy(app)


# ─── Modèles ──────────────────────────────────────────────────────────────────

class Article(db.Model):
    __tablename__ = "articles"

    id = db.Column(db.Integer, primary_key=True)
    titre = db.Column(db.String(200), nullable=False)
    contenu = db.Column(db.Text, nullable=False)
    auteur = db.Column(db.String(100), nullable=False, default="Anonyme")
    categorie = db.Column(db.String(50), nullable=False, default="Général")
    date_creation = db.Column(db.DateTime, default=datetime.utcnow)
    date_modification = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    actif = db.Column(db.Boolean, default=True)

    def to_dict(self):
        return {
            "id": self.id,
            "titre": self.titre,
            "contenu": self.contenu,
            "auteur": self.auteur,
            "categorie": self.categorie,
            "date_creation": self.date_creation.isoformat() if self.date_creation else None,
            "date_modification": self.date_modification.isoformat() if self.date_modification else None,
            "actif": self.actif,
        }


class Contact(db.Model):
    __tablename__ = "contacts"

    id = db.Column(db.Integer, primary_key=True)
    nom = db.Column(db.String(100), nullable=False)
    email = db.Column(db.String(150), nullable=False)
    message = db.Column(db.Text, nullable=False)
    date_envoi = db.Column(db.DateTime, default=datetime.utcnow)
    lu = db.Column(db.Boolean, default=False)

    def to_dict(self):
        return {
            "id": self.id,
            "nom": self.nom,
            "email": self.email,
            "message": self.message,
            "date_envoi": self.date_envoi.isoformat() if self.date_envoi else None,
            "lu": self.lu,
        }


# ─── Routes Frontend ──────────────────────────────────────────────────────────

@app.route("/")
def index():
    return render_template("index.html")


@app.route("/articles")
def articles_page():
    return render_template("articles.html")


@app.route("/contact")
def contact_page():
    return render_template("contact.html")


# ─── API Articles ─────────────────────────────────────────────────────────────

@app.route("/api/articles", methods=["GET"])
def get_articles():
    categorie = request.args.get("categorie")
    query = Article.query.filter_by(actif=True)
    if categorie:
        query = query.filter_by(categorie=categorie)
    articles = query.order_by(Article.date_creation.desc()).all()
    return jsonify({"success": True, "data": [a.to_dict() for a in articles], "total": len(articles)})


@app.route("/api/articles/<int:article_id>", methods=["GET"])
def get_article(article_id):
    article = Article.query.get_or_404(article_id)
    return jsonify({"success": True, "data": article.to_dict()})


@app.route("/api/articles", methods=["POST"])
def create_article():
    data = request.get_json()
    if not data or not data.get("titre") or not data.get("contenu"):
        return jsonify({"success": False, "error": "titre et contenu requis"}), 400

    article = Article(
        titre=data["titre"],
        contenu=data["contenu"],
        auteur=data.get("auteur", "Anonyme"),
        categorie=data.get("categorie", "Général"),
    )
    db.session.add(article)
    db.session.commit()
    logger.info(f"Article créé : {article.id} - {article.titre}")
    return jsonify({"success": True, "data": article.to_dict()}), 201


@app.route("/api/articles/<int:article_id>", methods=["PUT"])
def update_article(article_id):
    article = Article.query.get_or_404(article_id)
    data = request.get_json()
    if "titre" in data:
        article.titre = data["titre"]
    if "contenu" in data:
        article.contenu = data["contenu"]
    if "auteur" in data:
        article.auteur = data["auteur"]
    if "categorie" in data:
        article.categorie = data["categorie"]
    article.date_modification = datetime.utcnow()
    db.session.commit()
    return jsonify({"success": True, "data": article.to_dict()})


@app.route("/api/articles/<int:article_id>", methods=["DELETE"])
def delete_article(article_id):
    article = Article.query.get_or_404(article_id)
    article.actif = False
    db.session.commit()
    return jsonify({"success": True, "message": "Article archivé"})


# ─── API Contact ──────────────────────────────────────────────────────────────

@app.route("/api/contact", methods=["POST"])
def submit_contact():
    data = request.get_json()
    if not data or not all(k in data for k in ["nom", "email", "message"]):
        return jsonify({"success": False, "error": "nom, email et message requis"}), 400

    contact = Contact(
        nom=data["nom"],
        email=data["email"],
        message=data["message"],
    )
    db.session.add(contact)
    db.session.commit()
    logger.info(f"Nouveau message de {contact.nom} ({contact.email})")
    return jsonify({"success": True, "message": "Message envoyé avec succès"}), 201


# ─── Health Check ─────────────────────────────────────────────────────────────

@app.route("/health")
def health():
    try:
        db.session.execute(db.text("SELECT 1"))
        db_status = "ok"
    except Exception as e:
        db_status = f"erreur: {str(e)}"
    return jsonify({
        "status": "ok" if db_status == "ok" else "degraded",
        "database": db_status,
        "timestamp": datetime.utcnow().isoformat(),
    })


@app.route("/api/stats", methods=["GET"])
def get_stats():
    return jsonify({
        "articles": Article.query.filter_by(actif=True).count(),
        "contacts": Contact.query.count(),
        "categories": db.session.query(Article.categorie).distinct().count(),
    })


# ─── Init DB ──────────────────────────────────────────────────────────────────

def init_db():
    with app.app_context():
        db.create_all()
        if Article.query.count() == 0:
            articles = [
                Article(titre="Bienvenue sur notre plateforme", contenu="Ceci est le premier article de notre application déployée sur AWS avec une architecture 3-tiers sécurisée.", auteur="Admin", categorie="Actualités"),
                Article(titre="Architecture AWS sécurisée", contenu="Notre infrastructure utilise un VPC avec 3 sous-réseaux isolés : public, privé applicatif et privé base de données. Chaque couche est protégée par des Security Groups et des ACL réseau.", auteur="Équipe Technique", categorie="Technique"),
                Article(titre="Conteneurisation avec Docker", contenu="Chaque composant (web, backend, database) tourne dans son propre conteneur Docker sur des instances EC2 dédiées dans des sous-réseaux appropriés.", auteur="DevOps", categorie="Technique"),
            ]
            db.session.add_all(articles)
            db.session.commit()
            logger.info("Données initiales insérées")


if __name__ == "__main__":
    init_db()
    app.run(host="0.0.0.0", port=5000, debug=os.environ.get("FLASK_DEBUG", "false").lower() == "true")
