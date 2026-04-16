import os
import boto3
from botocore.exceptions import ClientError, NoCredentialsError
from flask import Flask, render_template, request, jsonify, redirect, url_for, flash
from werkzeug.utils import secure_filename
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
app.secret_key = os.environ.get("SECRET_KEY", "change-me-in-production")

# AWS Configuration
AWS_ACCESS_KEY_ID = os.environ.get("AWS_ACCESS_KEY_ID")
AWS_SECRET_ACCESS_KEY = os.environ.get("AWS_SECRET_ACCESS_KEY")
AWS_REGION = os.environ.get("AWS_REGION", "eu-west-3")
S3_BUCKET = os.environ.get("S3_BUCKET_NAME")

ALLOWED_EXTENSIONS = {"txt", "pdf", "png", "jpg", "jpeg", "gif", "csv", "json", "zip", "docx", "xlsx"}
MAX_FILE_SIZE = 50 * 1024 * 1024  # 50MB


def get_s3_client():
    return boto3.client(
        "s3",
        aws_access_key_id=AWS_ACCESS_KEY_ID,
        aws_secret_access_key=AWS_SECRET_ACCESS_KEY,
        region_name=AWS_REGION,
    )


def allowed_file(filename):
    return "." in filename and filename.rsplit(".", 1)[1].lower() in ALLOWED_EXTENSIONS


def format_size(size_bytes):
    if size_bytes < 1024:
        return f"{size_bytes} B"
    elif size_bytes < 1024 ** 2:
        return f"{size_bytes / 1024:.1f} KB"
    elif size_bytes < 1024 ** 3:
        return f"{size_bytes / (1024 ** 2):.1f} MB"
    else:
        return f"{size_bytes / (1024 ** 3):.1f} GB"


@app.route("/")
def index():
    return render_template("index.html", bucket=S3_BUCKET, region=AWS_REGION)


@app.route("/api/files", methods=["GET"])
def list_files():
    try:
        s3 = get_s3_client()
        response = s3.list_objects_v2(Bucket=S3_BUCKET)
        files = []
        for obj in response.get("Contents", []):
            files.append({
                "key": obj["Key"],
                "name": obj["Key"].split("/")[-1],
                "size": format_size(obj["Size"]),
                "size_bytes": obj["Size"],
                "last_modified": obj["LastModified"].strftime("%d/%m/%Y %H:%M"),
                "etag": obj["ETag"].strip('"'),
            })
        files.sort(key=lambda x: x["last_modified"], reverse=True)
        return jsonify({"success": True, "files": files, "count": len(files)})
    except ClientError as e:
        logger.error(f"Erreur S3 list: {e}")
        return jsonify({"success": False, "error": str(e)}), 500
    except NoCredentialsError:
        return jsonify({"success": False, "error": "Identifiants AWS manquants"}), 401


@app.route("/api/upload", methods=["POST"])
def upload_file():
    if "file" not in request.files:
        return jsonify({"success": False, "error": "Aucun fichier fourni"}), 400

    file = request.files["file"]
    if file.filename == "":
        return jsonify({"success": False, "error": "Nom de fichier vide"}), 400

    if not allowed_file(file.filename):
        return jsonify({"success": False, "error": f"Extension non autorisée"}), 400

    try:
        filename = secure_filename(file.filename)
        file.seek(0, 2)
        file_size = file.tell()
        file.seek(0)

        if file_size > MAX_FILE_SIZE:
            return jsonify({"success": False, "error": "Fichier trop grand (max 50MB)"}), 400

        s3 = get_s3_client()
        s3.upload_fileobj(
            file,
            S3_BUCKET,
            filename,
            ExtraArgs={"ContentType": file.content_type or "application/octet-stream"},
        )
        logger.info(f"Fichier uploadé: {filename}")
        return jsonify({"success": True, "message": f"'{filename}' uploadé avec succès", "filename": filename})
    except ClientError as e:
        logger.error(f"Erreur upload S3: {e}")
        return jsonify({"success": False, "error": str(e)}), 500


@app.route("/api/download/<path:key>", methods=["GET"])
def download_file(key):
    try:
        s3 = get_s3_client()
        url = s3.generate_presigned_url(
            "get_object",
            Params={"Bucket": S3_BUCKET, "Key": key},
            ExpiresIn=300,
        )
        return jsonify({"success": True, "url": url})
    except ClientError as e:
        logger.error(f"Erreur génération URL: {e}")
        return jsonify({"success": False, "error": str(e)}), 500


@app.route("/api/delete/<path:key>", methods=["DELETE"])
def delete_file(key):
    """
    Cette route simule une tentative de suppression.
    Le compte de service n'a PAS les droits s3:DeleteObject.
    L'erreur AWS sera renvoyée telle quelle pour démonstration.
    """
    try:
        s3 = get_s3_client()
        s3.delete_object(Bucket=S3_BUCKET, Key=key)
        return jsonify({"success": True, "message": f"'{key}' supprimé"})
    except ClientError as e:
        error_code = e.response["Error"]["Code"]
        logger.warning(f"Tentative de suppression refusée par AWS: {error_code} pour '{key}'")
        if error_code == "AccessDenied":
            return jsonify({
                "success": False,
                "error": "Accès refusé",
                "detail": "Le compte de service ne dispose pas de la permission s3:DeleteObject. Action non autorisée par la politique IAM.",
                "aws_code": error_code,
            }), 403
        return jsonify({"success": False, "error": str(e)}), 500


@app.route("/health")
def health():
    return jsonify({"status": "ok", "bucket": S3_BUCKET, "region": AWS_REGION})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=os.environ.get("FLASK_DEBUG", "false").lower() == "true")
