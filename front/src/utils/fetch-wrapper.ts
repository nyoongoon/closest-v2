import {useAuthStore} from '@/stores';

export const fetchWrapper = {
    get: request('GET'),
    post: request('POST'),
    put: request('PUT'),
    delete: request('DELETE')
};

function request(method: string) {
    return (url: string, body: any, options: { credentials?: any } = {}) => {
        const {credentials} = options;
        const requestOptions = {
            method,
            headers: authHeader(url),
            body,
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
          .catch((error)=>{
              console.log(error);
              alert(error.response.data.message);
          });
    }
}

// helper functions

function authHeader(url: string): { [key: string]: string } {
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

function handleResponse(response: any) {
    return response.text().then((text: string | undefined | null) => {
        const data = text && JSON.parse(text);

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
