// app.js - Frontend JavaScript pour AppCloud AWS

const API_BASE = "/api";

// ─── Utilitaires ──────────────────────────────────────────────────────────────

function showMessage(elementId, text, type = "success") {
    const el = document.getElementById(elementId);
    if (!el) return;
    el.textContent = text;
    el.className = `message ${type}`;
    setTimeout(() => { el.className = "message"; }, 5000);
}

function formatDate(isoString) {
    if (!isoString) return "—";
    return new Date(isoString).toLocaleDateString("fr-FR", {
        day: "2-digit", month: "2-digit", year: "numeric",
        hour: "2-digit", minute: "2-digit"
    });
}

// ─── Stats ────────────────────────────────────────────────────────────────────

async function loadStats() {
    try {
        const res = await fetch(`${API_BASE}/stats`);
        const data = await res.json();
        const set = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };
        set("stat-articles", data.articles ?? "—");
        set("stat-contacts", data.contacts ?? "—");
        set("stat-categories", data.categories ?? "—");
    } catch (err) {
        console.error("Erreur chargement stats:", err);
    }
}

// ─── Health ───────────────────────────────────────────────────────────────────

async function loadHealth() {
    const el = document.getElementById("health-status");
    if (!el) return;
    try {
        const res = await fetch("/health");
        const data = await res.json();
        const isOk = data.status === "ok";
        el.className = `health-card ${isOk ? "health-ok" : "health-err"}`;
        el.innerHTML = `
            <p><strong>Statut global :</strong> ${isOk ? "✅ Opérationnel" : "⚠️ Dégradé"}</p>
            <p><strong>Base de données :</strong> ${data.database}</p>
            <p><strong>Horodatage :</strong> ${formatDate(data.timestamp)}</p>
        `;
    } catch (err) {
        el.className = "health-card health-err";
        el.innerHTML = `<p>❌ Impossible de contacter le backend</p>`;
    }
}

// ─── Articles ─────────────────────────────────────────────────────────────────

async function loadArticles() {
    const container = document.getElementById("articles-list");
    if (!container) return;
    container.innerHTML = '<p class="loading">Chargement des articles...</p>';

    try {
        const categorie = document.getElementById("filter-categorie")?.value || "";
        const url = categorie ? `${API_BASE}/articles?categorie=${encodeURIComponent(categorie)}` : `${API_BASE}/articles`;
        const res = await fetch(url);
        const data = await res.json();

        if (!data.success || data.data.length === 0) {
            container.innerHTML = '<p class="loading">Aucun article trouvé.</p>';
            return;
        }

        container.innerHTML = data.data.map(article => `
            <div class="article-card" id="article-${article.id}">
                <h3>${escapeHtml(article.titre)}</h3>
                <div class="article-meta">
                    <span class="badge">${escapeHtml(article.categorie)}</span>
                    <span>✍️ ${escapeHtml(article.auteur)}</span>
                    <span>📅 ${formatDate(article.date_creation)}</span>
                </div>
                <p>${escapeHtml(article.contenu)}</p>
                <div class="article-actions">
                    <button class="btn-danger" onclick="deleteArticle(${article.id})">🗑 Archiver</button>
                </div>
            </div>
        `).join("");
    } catch (err) {
        container.innerHTML = `<p class="loading" style="color:var(--danger)">Erreur : ${err.message}</p>`;
    }
}

async function createArticle() {
    const titre = document.getElementById("titre")?.value?.trim();
    const auteur = document.getElementById("auteur")?.value?.trim() || "Anonyme";
    const categorie = document.getElementById("categorie")?.value;
    const contenu = document.getElementById("contenu")?.value?.trim();

    if (!titre || !contenu) {
        showMessage("form-message", "Le titre et le contenu sont requis.", "error");
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/articles`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ titre, auteur, categorie, contenu }),
        });
        const data = await res.json();
        if (data.success) {
            showMessage("form-message", "Article publié avec succès !", "success");
            document.getElementById("titre").value = "";
            document.getElementById("contenu").value = "";
            document.getElementById("auteur").value = "";
            loadArticles();
        } else {
            showMessage("form-message", data.error || "Erreur lors de la création.", "error");
        }
    } catch (err) {
        showMessage("form-message", `Erreur réseau : ${err.message}`, "error");
    }
}

async function deleteArticle(id) {
    if (!confirm("Archiver cet article ?")) return;
    try {
        const res = await fetch(`${API_BASE}/articles/${id}`, { method: "DELETE" });
        const data = await res.json();
        if (data.success) {
            const el = document.getElementById(`article-${id}`);
            if (el) el.remove();
        }
    } catch (err) {
        alert("Erreur lors de l'archivage.");
    }
}

// ─── Contact ──────────────────────────────────────────────────────────────────

async function sendContact() {
    const nom = document.getElementById("contact-nom")?.value?.trim();
    const email = document.getElementById("contact-email")?.value?.trim();
    const message = document.getElementById("contact-message")?.value?.trim();

    if (!nom || !email || !message) {
        showMessage("contact-message-result", "Tous les champs sont requis.", "error");
        return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showMessage("contact-message-result", "Adresse email invalide.", "error");
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/contact`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ nom, email, message }),
        });
        const data = await res.json();
        if (data.success) {
            showMessage("contact-message-result", "Message envoyé avec succès !", "success");
            document.getElementById("contact-nom").value = "";
            document.getElementById("contact-email").value = "";
            document.getElementById("contact-message").value = "";
        } else {
            showMessage("contact-message-result", data.error || "Erreur lors de l'envoi.", "error");
        }
    } catch (err) {
        showMessage("contact-message-result", `Erreur réseau : ${err.message}`, "error");
    }
}

// ─── Sécurité ─────────────────────────────────────────────────────────────────

function escapeHtml(text) {
    if (!text) return "";
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
