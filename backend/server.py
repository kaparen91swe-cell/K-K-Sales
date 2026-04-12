from flask import Flask, request, jsonify
import time

app = Flask(__name__)

# En enkel minnesbaserad databas för att simulera data
db = {
    "orders": [],
    "inventory": [
        {"id": 1, "name": "Produkt A", "price": 10.0, "quantity": 50},
        {"id": 2, "name": "Produkt B", "price": 20.0, "quantity": 30},
    ]
}

@app.route('/orders/process', methods=['POST'])
def process_order():
    try:
        data = request.json
        user_id = data.get('user_id')
        product_id = data.get('product_id')
        quantity = data.get('quantity')

        print(f"Mottog beställning: Användare {user_id}, Produkt {product_id}, Antal {quantity}")

        # Simulera en lyckad transaktion
        order_id = int(time.time())
        db["orders"].append({
            "order_id": order_id,
            "user_id": user_id,
            "product_id": product_id,
            "quantity": quantity
        })

        return jsonify({
            "success": True,
            "message": "Beställningen har behandlats korrekt!",
            "transactionId": order_id
        })
    except Exception as e:
        return jsonify({
            "success": False,
            "message": f"Serverfel: {str(e)}"
        }), 500

@app.route('/api/status', methods=['GET'])
def get_status():
    return jsonify({"status": "Servern körs!"})

from waitress import serve

if __name__ == '__main__':
    print("Servern körs nu i produktionsläge på port 5000...")
    serve(app, host='0.0.0.0', port=5000)