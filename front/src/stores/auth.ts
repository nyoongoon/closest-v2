import {defineStore} from 'pinia';

import {fetchWrapper} from '@/utils';
import {useRouter} from "vue-router";

const router = useRouter();
const baseUrl = `/api/member/auth`;

type User = {
    id: number
    username: string
    firstName: string
    lastName: string
    jwtToken: string
}

export const useAuthStore = defineStore({
    id: 'auth',
    state: (): { user: User | null, refreshTokenTimeout: number | null } => ({
        user: null,
        refreshTokenTimeout: null
    }),
    actions: {
        async login(email: string, password: string) {
            this.user = await fetchWrapper.post(`${baseUrl}/signin`, {
                email,
                password
            }, {credentials: 'include'});
            this.startRefreshTokenTimer();
        },
        logout() {
            fetchWrapper.post(`${baseUrl}/revoke-token`, {}, {credentials: 'include'});
            this.stopRefreshTokenTimer();
            this.user = null;
            router.push('/login');
        },
        async refreshToken() {
            this.user = await fetchWrapper.post(`${baseUrl}/refresh-token`, {}, {credentials: 'include'});
            this.startRefreshTokenTimer();
        },
        startRefreshTokenTimer() {
            // parse json object from base64 encoded jwt token
            const jwtBase64 = this.user?.jwtToken?.split('.')[1];

            if (jwtBase64) {
                const jwtToken = JSON.parse(atob(jwtBase64));

                // set a timeout to refresh the token a minute before it expires
                const expires = new Date(jwtToken.exp * 1000);
                const timeout = expires.getTime() - Date.now() - (60 * 1000);
                this.refreshTokenTimeout = setTimeout(this.refreshToken, timeout);
            }
        },
        stopRefreshTokenTimer() {
            if (this.refreshTokenTimeout !== null) {
                clearTimeout(this.refreshTokenTimeout);
            }
        }
    }
});
