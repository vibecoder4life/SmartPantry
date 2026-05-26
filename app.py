
from flask import Flask, request, jsonify
from flask_cors import CORS
import mysql.connector
import bcrypt
import os

app = Flask(__name__)
CORS(app)  # Allow requests from Android emulator / device

# ── Database configuration ───────────────────────────────────────────────────
DB_CONFIG = {
    "host":     os.getenv("DB_HOST",     "localhost"),
    "port":     int(os.getenv("DB_PORT", 3307)),
    "user":     os.getenv("DB_USER",     "root"),
    "password": os.getenv("DB_PASSWORD", ""),          # ← set your MySQL password
    "database": os.getenv("DB_NAME",     "smartpantry_db"),
}


def get_db():
    """Return a fresh MySQL connection."""
    return mysql.connector.connect(**DB_CONFIG)


# ── Auth endpoints ────────────────────────────────────────────────────────────

@app.route("/register", methods=["POST"])
def register():
    data = request.get_json(force=True)
    full_name = data.get("full_name", "").strip()
    email     = data.get("email",     "").strip()
    username  = data.get("username",  "").strip()
    password  = data.get("password",  "").strip()

    if not all([full_name, email, username, password]):
        return jsonify({"error": "All fields are required"}), 400

    # Always store passwords as bcrypt hash
    hashed = bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()

    try:
        conn   = get_db()
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO users (full_name, email, username, password) VALUES (%s,%s,%s,%s)",
            (full_name, email, username, hashed),
        )
        conn.commit()
        return jsonify({"message": "Account created successfully"}), 201
    except mysql.connector.IntegrityError:
        return jsonify({"error": "Username or email already exists"}), 409
    finally:
        cursor.close()
        conn.close()


@app.route("/login", methods=["POST"])
def login():
    data     = request.get_json(force=True)
    username = data.get("username", "").strip()
    password = data.get("password", "").strip()

    if not username or not password:
        return jsonify({"error": "Username and password required"}), 400

    conn   = get_db()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM users WHERE username = %s", (username,))
    user = cursor.fetchone()
    cursor.close()
    conn.close()

    if not user:
        return jsonify({"error": "Invalid credentials"}), 401

    stored = user["password"]
    # Support both bcrypt hashes and plain-text passwords (legacy)
    try:
        password_ok = bcrypt.checkpw(password.encode(), stored.encode())
    except Exception:
        password_ok = (password == stored)

    if not password_ok:
        return jsonify({"error": "Invalid credentials"}), 401

    return jsonify({
        "message":  "Login successful",
        "user_id":  user["id"],
        "username": user["username"],
        "full_name": user["full_name"],
    }), 200


@app.route("/reset-password", methods=["POST"])
def reset_password():
    data         = request.get_json(force=True)
    username     = data.get("username",     "").strip()
    new_password = data.get("new_password", "").strip()

    if not username or not new_password:
        return jsonify({"error": "Username and new_password required"}), 400

    hashed = bcrypt.hashpw(new_password.encode(), bcrypt.gensalt()).decode()

    conn   = get_db()
    cursor = conn.cursor()
    cursor.execute(
        "UPDATE users SET password = %s WHERE username = %s",
        (hashed, username),
    )
    affected = cursor.rowcount
    conn.commit()
    cursor.close()
    conn.close()

    if affected == 0:
        return jsonify({"error": "Username not found"}), 404

    return jsonify({"message": "Password updated successfully"}), 200


# ── Ingredients / Pantry endpoints ───────────────────────────────────────────

@app.route("/ingredients", methods=["GET"])
def get_ingredients():
    user_id = request.args.get("user_id")
    if not user_id:
        return jsonify({"error": "user_id query param required"}), 400

    conn   = get_db()
    cursor = conn.cursor(dictionary=True)
    cursor.execute(
        "SELECT id, user_id, ingredient_name, price, is_available, image_url, created_at "
        "FROM ingredients WHERE user_id = %s ORDER BY created_at DESC",
        (user_id,),
    )
    rows = cursor.fetchall()
    cursor.close()
    conn.close()

    # Convert Decimal → float and datetime → str for JSON serialisation
    for row in rows:
        row["price"]      = float(row["price"])
        row["created_at"] = str(row["created_at"])

    return jsonify(rows), 200


@app.route("/ingredients", methods=["POST"])
def add_ingredient():
    data            = request.get_json(force=True)
    user_id         = data.get("user_id")
    ingredient_name = data.get("ingredient_name", "").strip()
    price           = data.get("price", 0)
    is_available    = data.get("is_available", 1)
    image_url       = data.get("image_url", None)

    if not user_id or not ingredient_name:
        return jsonify({"error": "user_id and ingredient_name required"}), 400

    conn   = get_db()
    cursor = conn.cursor()
    cursor.execute(
        "INSERT INTO ingredients (user_id, ingredient_name, price, is_available, image_url) "
        "VALUES (%s,%s,%s,%s,%s)",
        (user_id, ingredient_name, price, is_available, image_url),
    )
    new_id = cursor.lastrowid
    conn.commit()
    cursor.close()
    conn.close()

    return jsonify({"message": "Ingredient added", "id": new_id}), 201


@app.route("/ingredients/<int:ingredient_id>", methods=["PUT"])
def update_ingredient(ingredient_id):
    data            = request.get_json(force=True)
    ingredient_name = data.get("ingredient_name", "").strip()
    price           = data.get("price")
    is_available    = data.get("is_available")
    image_url       = data.get("image_url")

    if not ingredient_name or price is None:
        return jsonify({"error": "ingredient_name and price required"}), 400

    conn   = get_db()
    cursor = conn.cursor()
    cursor.execute(
        "UPDATE ingredients "
        "SET ingredient_name=%s, price=%s, is_available=%s, image_url=%s "
        "WHERE id=%s",
        (ingredient_name, price, is_available, image_url, ingredient_id),
    )
    conn.commit()
    cursor.close()
    conn.close()

    return jsonify({"message": "Ingredient updated"}), 200


@app.route("/ingredients/<int:ingredient_id>", methods=["DELETE"])
def delete_ingredient(ingredient_id):
    conn   = get_db()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM ingredients WHERE id = %s", (ingredient_id,))
    conn.commit()
    cursor.close()
    conn.close()

    return jsonify({"message": "Ingredient deleted"}), 200


# ── Entry point ───────────────────────────────────────────────────────────────

if __name__ == "__main__":
    # host="0.0.0.0" makes the server reachable from your Android device / emulator
    app.run(host="0.0.0.0", port=5000, debug=True)