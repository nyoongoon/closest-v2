package config

import (
	"os"
)

type Config struct {
	ServerPort      string
	DBPath          string
	AccessSecretKey  string
	RefreshSecretKey string
}

func Load() *Config {
	return &Config{
		ServerPort:       getEnv("SERVER_PORT", "8080"),
		DBPath:           getEnv("DB_PATH", "closest.db"),
		AccessSecretKey:  getEnv("JWT_ACCESS_SECRET_KEY", "default-access-secret-key-for-dev"),
		RefreshSecretKey: getEnv("JWT_REFRESH_SECRET_KEY", "default-refresh-secret-key-for-dev"),
	}
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
