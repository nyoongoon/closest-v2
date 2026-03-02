import {useAuthStore} from '@/stores';
import {toastBus} from '@/composables/useToast';

export const fetchWrapper = {
    get: request('GET'),
    post: request('POST'),
    put: request('PUT'),
    delete: request('DELETE')
};

function request(method: string) {
    return (url: string, body: any, options: { credentials?: any } = {}) => {
        const {credentials} = options;
        const requestOptions: RequestInit & { headers: Record<string, string> } = {
            method,
            headers: authHeader(url),
            credentials,
        };
        if (body) {
            requestOptions.headers['Content-Type'] = 'application/json';
            requestOptions.body = JSON.stringify(body);
        }
        if (credentials) {
            requestOptions.credentials = credentials;
        }
        return fetch(url, requestOptions)
            .then(handleResponse)
            .catch((error: unknown) => {
                console.error(error);
                const message = typeof error === 'string' ? error : '요청 중 오류가 발생했습니다.';
                toastBus.emit('show', { message, type: 'error' });
                return Promise.reject(error);
            });
    };
}

// helper functions

function authHeader(url: string): Record<string, string> {
    // return auth header with jwt if user is logged in and request is to the api url
    const {user} = useAuthStore();
    const isLoggedIn = !!user?.jwtToken;
    const isApiUrl = url.startsWith(import.meta.env.VITE_API_URL);
    if (isLoggedIn && isApiUrl) {
        return {Authorization: `Bearer ${user.jwtToken}`};
    } else {
        return {};
    }
}

function handleResponse(response: Response) {
    return response.text().then((text: string) => {
        const data = text ? JSON.parse(text) : null;

        if (!response.ok) {
            const {user, logout} = useAuthStore();
            if ([401, 403].includes(response.status) && user) {
                // auto logout if 401 Unauthorized or 403 Forbidden response returned from api
                logout();
            }

            const error = (data && data.message) || response.statusText;
            return Promise.reject(error);
        }

        return data;
    });
}
