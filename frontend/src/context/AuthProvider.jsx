import {
    createContext,
    useContext,
    useEffect,
    useState
} from "react";
import { login as performLogin } from '../api/client.js';
import jwtDecode from "jwt-decode";

const AuthContext = createContext({});

const AuthProvider = ({ children }) => {

    const [user, setUser] = useState(null);

    const setUserFromToken = () => {
        const token = localStorage.getItem("access_token");
        if (token) {
            try {
                const decodedToken = jwtDecode(token);
                setUser({
                    email: decodedToken.sub,
                    roles: decodedToken.scopes || (decodedToken.scope ? [decodedToken.scope] : ['ROLE_USER'])
                });
            } catch (e) {
                console.error("Error decoding token:", e);
                localStorage.removeItem("access_token");
                setUser(null);
            }
        }
    };

    useEffect(() => {
        setUserFromToken();
    }, []);

    const login = async (credentials) => {
        return new Promise((resolve, reject) => {
            performLogin(credentials).then(response => {
                // Token might be in response.data.token or response.headers.authorization
                const token = response.data?.token || response.headers?.authorization;
                if (token) {
                    localStorage.setItem("access_token", token);
                    try {
                        const decodedToken = jwtDecode(token);
                        setUser({
                            email: decodedToken.sub,
                            roles: decodedToken.scopes || (decodedToken.scope ? [decodedToken.scope] : ['ROLE_USER'])
                        });
                    } catch (e) {
                        console.error("Error decoding token:", e);
                    }
                }
                resolve(response);
            }).catch(err => {
                reject(err);
            });
        });
    };

    const logOut = () => {
        localStorage.removeItem("access_token");
        setUser(null);
    };

    const isAuthenticated = () => {
        const token = localStorage.getItem("access_token");
        if (!token) {
            return false;
        }
        try {
            const { exp: expiration } = jwtDecode(token);
            if (Date.now() > expiration * 1000) {
                logOut();
                return false;
            }
            return true;
        } catch (e) {
            logOut();
            return false;
        }
    };

    return (
        <AuthContext.Provider value={{
            user,
            login,
            logOut,
            isAuthenticated,
            setUserFromToken
        }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);

export default AuthProvider;
