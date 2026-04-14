from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
import json
import time
import os
import requests
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)

# --- CONFIGURATION ---
GITHUB_TOKEN = os.environ.get('GITHUB_TOKEN')
GITHUB_REPO = "kaparen91swe-cell/K-K-Sales"
GITHUB_WORKFLOW_FILE = "android_build.yml"

# Bitcoin Settings
BTC_XPUB = os.environ.get('BTC_XPUB', 'DIN_XPUB_HÄR')
BTC_WALLET_ADDRESS = os.environ.get('BTC_WALLET_ADDRESS', 'DIN_BTC_ADRESS_HÄR')

if not GITHUB_TOKEN:
    print("WARNING: GITHUB_TOKEN environment variable is not set!")
    print("GitHub deployment features will not work.")

# Database Configuration
basedir = os.path.abspath(os.path.dirname(__file__))
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///' + os.path.join(basedir, 'kksales_online.db')
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db = SQLAlchemy(app)

# --- DATABASE MODELS ---

class Payment(db.Model):
    __tablename__ = 'payments'
    id = db.Column(db.Integer, primary_key=True)
    userId = db.Column(db.Integer)
    amount_sek = db.Column(db.Float)
    amount_btc = db.Column(db.Float)
    address = db.Column(db.String(100))
    status = db.Column(db.String(50), default='pending') # pending, confirmed, failed
    timestamp = db.Column(db.BigInteger)

    def to_dict(self):
        return {
            "id": self.id,
            "userId": self.userId,
            "amount_sek": self.amount_sek,
            "amount_btc": self.amount_btc,
            "address": self.address,
            "status": self.status,
            "timestamp": self.timestamp
        }

class User(db.Model):
    __tablename__ = 'users'
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), unique=True, nullable=False)
    password = db.Column(db.String(100))
    balance = db.Column(db.Float, default=0.0)
    cashBalance = db.Column(db.Float, default=0.0)
    isAdmin = db.Column(db.Boolean, default=False)
    isAdminPlus = db.Column(db.Boolean, default=False)
    isReseller = db.Column(db.Boolean, default=False)
    isLageransvarig = db.Column(db.Boolean, default=False)
    isTransportor = db.Column(db.Boolean, default=False)
    role = db.Column(db.String(100))
    profileIcon = db.Column(db.String(100))
    vehicleType = db.Column(db.String(100))
    
    fuelPrice = db.Column(db.Float)
    fuelConsumption = db.Column(db.Float)
    vehicleBonusPerUnit = db.Column(db.Float)
    vehicleFeePerUnit = db.Column(db.Float)
    
    productCommissions = db.Column(db.Text, default='{}')
    productResellerPrices = db.Column(db.Text, default='{}')

    def to_dict(self):
        return {
            "id": self.id,
            "name": self.name,
            "password": self.password,
            "balance": self.balance,
            "cashBalance": self.cashBalance,
            "isAdmin": self.isAdmin,
            "isAdminPlus": self.isAdminPlus,
            "isReseller": self.isReseller,
            "isLageransvarig": self.isLageransvarig,
            "isTransportor": self.isTransportor,
            "role": self.role,
            "profileIcon": self.profileIcon,
            "vehicleType": self.vehicleType,
            "fuelPrice": self.fuelPrice,
            "fuelConsumption": self.fuelConsumption,
            "vehicleBonusPerUnit": self.vehicleBonusPerUnit,
            "vehicleFeePerUnit": self.vehicleFeePerUnit,
            "productCommissions": json.loads(self.productCommissions or '{}'),
            "productResellerPrices": json.loads(self.productResellerPrices or '{}')
        }

class Product(db.Model):
    __tablename__ = 'products'
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(200), nullable=False)
    unitCost = db.Column(db.Float, default=0.0)
    salesPrice = db.Column(db.Float, default=0.0)
    resellerPrice = db.Column(db.Float, default=0.0)
    quantity = db.Column(db.Integer, default=0)
    unit = db.Column(db.String(50), default='g')
    imageUri = db.Column(db.String(500))
    bulkPrices = db.Column(db.Text, default='[]')
    lowStockThreshold = db.Column(db.Integer, default=500)

    def to_dict(self):
        return {
            "id": self.id,
            "name": self.name,
            "unitCost": self.unitCost,
            "salesPrice": self.salesPrice,
            "resellerPrice": self.resellerPrice,
            "quantity": self.quantity,
            "unit": self.unit,
            "imageUri": self.imageUri,
            "bulkPrices": json.loads(self.bulkPrices or '[]'),
            "lowStockThreshold": self.lowStockThreshold
        }

class Transaction(db.Model):
    __tablename__ = 'transactions'
    id = db.Column(db.Integer, primary_key=True)
    userId = db.Column(db.Integer)
    productId = db.Column(db.Integer)
    amount = db.Column(db.Float)
    vatAmount = db.Column(db.Float, default=0.0)
    vatRate = db.Column(db.Float, default=0.0)
    quantity = db.Column(db.Integer, default=0)
    timestamp = db.Column(db.BigInteger)
    category = db.Column(db.String(100))
    type = db.Column(db.String(100))
    paymentMethod = db.Column(db.String(100))
    description = db.Column(db.String(1000))

    def to_dict(self):
        return {
            "id": self.id,
            "userId": self.userId,
            "productId": self.productId,
            "amount": self.amount,
            "vatAmount": self.vatAmount,
            "vatRate": self.vatRate,
            "quantity": self.quantity,
            "timestamp": self.timestamp,
            "category": self.category,
            "type": self.type,
            "paymentMethod": self.paymentMethod,
            "description": self.description
        }

# --- API ENDPOINTS ---

@app.route('/status', methods=['GET'])
def get_status():
    return jsonify({"status": "online", "time": int(time.time() * 1000)})

@app.route('/users', methods=['GET'])
def get_users():
    users = User.query.all()
    return jsonify([u.to_dict() for u in users])

@app.route('/users/register', methods=['POST'])
def register():
    data = request.json
    user = User(
        name=data['name'],
        password=data.get('password'),
        isAdmin=data.get('isAdmin', False),
        isAdminPlus=data.get('isAdminPlus', False),
        isReseller=data.get('isReseller', False),
        isLageransvarig=data.get('isLageransvarig', False),
        isTransportor=data.get('isTransportor', False),
        role=data.get('role'),
        profileIcon=data.get('profileIcon'),
        vehicleType=data.get('vehicleType'),
        fuelPrice=data.get('fuelPrice'),
        fuelConsumption=data.get('fuelConsumption'),
        vehicleBonusPerUnit=data.get('vehicleBonusPerUnit'),
        vehicleFeePerUnit=data.get('vehicleFeePerUnit'),
        productCommissions=json.dumps(data.get('productCommissions', {})),
        productResellerPrices=json.dumps(data.get('productResellerPrices', {}))
    )
    db.session.add(user)
    db.session.commit()
    return jsonify(user.to_dict())

@app.route('/users/<int:id>', methods=['PUT'])
def update_user(id):
    user = db.session.get(User, id)
    if not user:
        return jsonify({"success": False, "message": "User not found"}), 404
    data = request.json
    for key, value in data.items():
        if key in ['productCommissions', 'productResellerPrices']:
            setattr(user, key, json.dumps(value))
        elif hasattr(user, key):
            setattr(user, key, value)
    db.session.commit()
    return jsonify({"success": True})

@app.route('/users/<int:id>', methods=['DELETE'])
def delete_user(id):
    user = db.session.get(User, id)
    if user:
        # Delete related data
        Transaction.query.filter_by(userId=id).delete()
        Payment.query.filter_by(userId=id).delete()
        db.session.delete(user)
        db.session.commit()
        return jsonify({"success": True, "message": "Användare raderad"})
    return jsonify({"success": False, "message": "Användare hittades inte"}), 404

@app.route('/products', methods=['GET'])
def get_products():
    prods = Product.query.all()
    return jsonify([p.to_dict() for p in prods])

@app.route('/products', methods=['POST'])
def sync_product():
    data = request.json
    prod = db.session.get(Product, data['id'])
    if not prod:
        prod = Product(id=data['id'])
        db.session.add(prod)
    
    prod.name = data['name']
    prod.quantity = data['quantity']
    prod.salesPrice = data['salesPrice']
    prod.resellerPrice = data['resellerPrice']
    prod.unit = data.get('unit', 'g')
    prod.unitCost = data.get('unitCost', 0.0)
    prod.lowStockThreshold = data.get('lowStockThreshold', 500)
    prod.bulkPrices = json.dumps(data.get('bulkPrices', []))
    db.session.commit()
    return jsonify({"success": True})

@app.route('/transactions', methods=['GET'])
def get_transactions():
    transactions = Transaction.query.all()
    return jsonify([t.to_dict() for t in transactions])

@app.route('/transactions', methods=['POST'])
def sync_transaction():
    data = request.json
    trans = Transaction(
        userId=data['userId'],
        productId=data['productId'],
        amount=data['amount'],
        vatAmount=data.get('vatAmount', 0.0),
        vatRate=data.get('vatRate', 0.0),
        quantity=data.get('quantity', 0),
        timestamp=data['timestamp'],
        category=data.get('category'),
        type=data.get('type'),
        paymentMethod=data.get('paymentMethod'),
        description=data.get('description')
    )
    db.session.add(trans)
    db.session.commit()
    return jsonify({"success": True})

@app.route('/orders/process', methods=['POST'])
def process_order():
    return jsonify({"success": True, "message": "Order mottagen av K&K Server"})

@app.route('/payments/btc/price', methods=['GET'])
def get_btc_price():
    try:
        response = requests.get("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=sek")
        data = response.json()
        return jsonify({"price_sek": data['bitcoin']['sek']})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/payments/btc/create', methods=['POST'])
def create_btc_payment():
    data = request.json
    userId = data.get('userId')
    amount_sek = data.get('amount_sek')
    
    try:
        price_response = requests.get("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=sek")
        btc_price = price_response.json()['bitcoin']['sek']
        amount_btc = amount_sek / btc_price
    except:
        return jsonify({"error": "Kunde inte hämta BTC-pris"}), 500

    address = BTC_WALLET_ADDRESS
    
    new_payment = Payment(
        userId=userId,
        amount_sek=amount_sek,
        amount_btc=amount_btc,
        address=address,
        status='pending',
        timestamp=int(time.time() * 1000)
    )
    db.session.add(new_payment)
    db.session.commit()
    
    return jsonify({
        "payment_id": new_payment.id,
        "address": address,
        "amount_btc": f"{amount_btc:.8f}",
        "amount_sek": amount_sek
    })

@app.route('/payments/btc/check/<int:payment_id>', methods=['GET'])
def check_btc_payment(payment_id):
    payment = db.session.get(Payment, payment_id)
    if not payment:
        return jsonify({"success": False, "message": "Payment not found"}), 404
    return jsonify(payment.to_dict())

@app.route('/admin/trigger-deploy', methods=['POST'])
def trigger_deploy():
    data = request.json
    headers = {
        "Authorization": f"token {GITHUB_TOKEN}",
        "Accept": "application/vnd.github.v3+json"
    }
    payload = {
        "ref": "main",
        "inputs": {
            "version_note": data.get("note", "Manual update from app"),
            "design_changes": json.dumps(data.get("changes", {}))
        }
    }
    url = f"https://api.github.com/repos/{GITHUB_REPO}/actions/workflows/{GITHUB_WORKFLOW_FILE}/dispatches"
    
    try:
        response = requests.post(url, headers=headers, json=payload)
        if response.status_code == 204:
            return jsonify({"success": True, "message": "GitHub Action triggad framgångsrikt!"})
        else:
            return jsonify({
                "success": False, 
                "message": f"Kunde inte trigga GitHub Action: {response.status_code}",
                "error": response.text
            }), 500
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500

if __name__ == '__main__':
    from waitress import serve
    with app.app_context():
        db.create_all()
    print("K&K Sales Online WSGI Server startad på port 8080...")
    serve(app, host='0.0.0.0', port=8080)
