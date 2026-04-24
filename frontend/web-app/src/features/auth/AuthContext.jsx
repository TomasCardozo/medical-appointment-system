import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { apiClient } from "../../shared/api/client";
import { getApiErrorMessage } from "../../shared/api/errors";
import { clearStoredToken, getStoredToken, setStoredToken } from "../../shared/auth/authStorage";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(getStoredToken());
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const logout = useCallback(() => {
    clearStoredToken();
    setToken(null);
    setUser(null);
  }, []);

  const refreshMe = useCallback(async () => {
    const meResponse = await apiClient.get("/auth/me");
    setUser(meResponse.data);
    return meResponse.data;
  }, []);

  useEffect(() => {
    let alive = true;

    async function bootstrap() {
      const storedToken = getStoredToken();
      if (!storedToken) {
        if (alive) {
          setLoading(false);
        }
        return;
      }

      try {
        await refreshMe();
        if (alive) {
          setToken(storedToken);
        }
      } catch {
        if (alive) {
          logout();
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    bootstrap();

    return () => {
      alive = false;
    };
  }, [logout, refreshMe]);

  const login = useCallback(async ({ email, password }) => {
    const loginResponse = await apiClient.post("/auth/login", { email, password });
    const nextToken = loginResponse.data.accessToken;

    setStoredToken(nextToken);
    setToken(nextToken);
    try {
      await refreshMe();
    } catch (error) {
      logout();
      throw new Error(getApiErrorMessage(error, "Could not load user profile"));
    }
  }, [logout, refreshMe]);

  const register = useCallback(async ({ role, fullName, email, password }) => {
    const rolePath = role === "DOCTOR" ? "doctor" : "patient";
    await apiClient.post(`/auth/register/${rolePath}`, { fullName, email, password });
  }, []);

  const value = useMemo(
    () => ({
      token,
      user,
      loading,
      isAuthenticated: Boolean(token && user),
      login,
      register,
      logout,
      refreshMe
    }),
    [token, user, loading, login, register, logout, refreshMe]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
