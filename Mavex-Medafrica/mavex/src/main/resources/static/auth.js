/**
 * auth.js — utilitaire JWT partagé
 * À inclure dans toutes les pages protégées via <script th:src="@{/js/auth.js}"></script>
 */

const Auth = {

    // ── Récupérer le token
    getToken() {
        return localStorage.getItem('accessToken');
    },

    getRefreshToken() {
        return localStorage.getItem('refreshToken');
    },

    // ── Vérifier si connecté, sinon rediriger vers login
    requireAuth() {
        if (!this.getToken()) {
            window.location.href = '/login';
            return false;
        }
        return true;
    },

    // ── Déconnexion
    async logout() {
        const refreshToken = this.getRefreshToken();
        try {
            await fetch('/api/auth/logout', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken })
            });
        } catch (e) {
            // on déconnecte quand même
        }
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userEmail');
        window.location.href = '/login';
    },

    // ── fetch avec JWT automatique + refresh si 401
    async fetchWithAuth(url, options = {}) {
        const token = this.getToken();

        const response = await fetch(url, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`,
                ...(options.headers || {})
            }
        });

        // Token expiré → tenter le refresh
        if (response.status === 401) {
            const refreshed = await this.tryRefresh();
            if (refreshed) {
                // Retry avec le nouveau token
                return fetch(url, {
                    ...options,
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${this.getToken()}`,
                        ...(options.headers || {})
                    }
                });
            } else {
                // Refresh échoué → retour login
                this.logout();
                return;
            }
        }

        return response;
    },

    // ── Tenter de rafraîchir le token
    async tryRefresh() {
        const refreshToken = this.getRefreshToken();
        if (!refreshToken) return false;

        try {
            const res = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken })
            });

            if (!res.ok) return false;

            const json = await res.json();
            localStorage.setItem('accessToken',  json.data.accessToken);
            localStorage.setItem('refreshToken', json.data.refreshToken);
            return true;

        } catch (e) {
            return false;
        }
    },

    // ── Récupérer l'email de l'utilisateur connecté
    getUserEmail() {
        return localStorage.getItem('userEmail') || '';
    }
};