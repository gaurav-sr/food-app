# Good Neighbour

A food-sharing app where neighbours can register, sign in, post what they made, say how many servings are available, and browse dishes shared by the community.

## Requirements

- Java 17+
- Maven 3.6+
- Node.js 20+
- npm

## Run the backend

```sh
cd backend
mvn spring-boot:run
```

The API runs at `http://localhost:8080`. SQLite creates `backend/foodshare.db` automatically; keep this file to retain accounts and food posts between restarts. Set `FOODSHARE_DB_PATH` to use a different database file.

## Run the frontend

```sh
cd frontend
npm install
npm run dev
```

Open the Vite URL (normally `http://localhost:5173`). To use a different API base URL, set `VITE_API_URL` before running the frontend.

## API

- `GET /api/food-posts` — list shared food posts, newest first.
- `POST /api/food-posts` — create a post (requires a bearer token). Required fields: `foodName` (1-100 characters) and `servings` (1-1000). `description` is optional (up to 500 characters).
- `POST /api/auth/register` — register with `displayName`, `email`, and a password of at least 8 characters.
- `POST /api/auth/login` — sign in with `email` and `password`.
- `GET /api/auth/me` — validate a bearer token and return the current user.
- `POST /api/auth/logout` — revoke the current bearer token.

Include the token returned by registration or login in protected requests as `Authorization: Bearer <token>`. Passwords are BCrypt-hashed before they are stored in SQLite. User accounts and food posts persist in `backend/foodshare.db`. Login tokens expire after 24 hours and remain in memory, so users must sign in again after a backend restart.
