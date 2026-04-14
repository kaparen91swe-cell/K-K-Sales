from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
import json
import time
import os
import requests
import logging
import subprocess
from dotenv import load_dotenv

# Ladda .env direkt vid start
load_dotenv()

# Konfigurera loggning för maximal synlighet
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - [%(levelname)s] - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# --- CONFIGURATION ---
GITHUB_TOKEN = os.environ.get('GITHUB_TOKEN')
GITHUB_REPO = "kaparen91swe-cell/K-K-Sales"
GITHUB_WORKFLOW_FILE = "android_build.yml"

# Bitcoin Settings
BTC_XPUB = os.environ.get('BTC_XPUB', 'DIN_XPUB_HÄR')
BTC_WALLET_ADDRESS = os.environ.get('BTC_WALLET_ADDRESS', 'DIN_BTC_ADRESS_HÄR')

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
    status = db.Column(db.String(50), default='pending')
    timestamp = db.Column(db.BigInteger)
    def to_dict(self):
        return {"id": self.id, "userId": self.userId, "amount_sek": self.amount_sek, "amount_btc": self.amount_btc, "address": self.address, "status": self.status, "timestamp": self.timestamp}

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
        return {"id": self.id, "name": self.name, "password": self.password, "balance": self.balance, "cashBalance": self.cashBalance, "isAdmin": self.isAdmin, "isAdminPlus": self.isAdminPlus, "isReseller": self.isReseller, "isLageransvarig": self.isLageransvarig, "isTransportor": self.isTransportor, "role": self.role, "profileIcon": self.profileIcon, "vehicleType": self.vehicleType, "fuelPrice": self.fuelPrice, "fuelConsumption": self.fuelConsumption, "vehicleBonusPerUnit": self.vehicleBonusPerUnit, "vehicleFeePerUnit": self.vehicleFeePerUnit, "productCommissions": json.loads(self.productCommissions or '{}'), "productResellerPrices": json.loads(self.productResellerPrices or '{}')}

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
        return {"id": self.id, "name": self.name, "unitCost": self.unitCost, "salesPrice": self.salesPrice, "resellerPrice": self.resellerPrice, "quantity": self.quantity, "unit": self.unit, "imageUri": self.imageUri, "bulkPrices": json.loads(self.bulkPrices or '[]'), "lowStockThreshold": self.lowStockThreshold}

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
        return {"id": self.id, "userId": self.userId, "productId": self.productId, "amount": self.amount, "vatAmount": self.vatAmount, "vatRate": self.vatRate, "quantity": self.quantity, "timestamp": self.timestamp, "category": self.category, "type": self.type, "paymentMethod": self.paymentMethod, "description": self.description}

# --- API ENDPOINTS ---
@app.route('/status', methods=['GET'])
def get_status(): return jsonify({"status": "online", "time": int(time.time() * 1000)})

@app.route('/users', methods=['GET'])
def get_users(): return jsonify([u.to_dict() for u in User.query.all()])

@app.route('/users/register', methods=['POST'])
def register():
    data = request.json
    try:
        user = User(name=data['name'], password=data.get('password'), isAdmin=data.get('isAdmin', False), isAdminPlus=data.get('isAdminPlus', False), isReseller=data.get('isReseller', False), isLageransvarig=data.get('isLageransvarig', False), isTransportor=data.get('isTransportor', False), role=data.get('role'), profileIcon=data.get('profileIcon'), vehicleType=data.get('vehicleType'), fuelPrice=data.get('fuelPrice'), fuelConsumption=data.get('fuelConsumption'), vehicleBonusPerUnit=data.get('vehicleBonusPerUnit'), vehicleFeePerUnit=data.get('vehicleFeePerUnit'), productCommissions=json.dumps(data.get('productCommissions', {})), productResellerPrices=json.dumps(data.get('productResellerPrices', {})))
        db.session.add(user)
        db.session.commit()
        return jsonify(user.to_dict())
    except Exception as e:
        db.session.rollback()
        return jsonify({"success": False, "message": str(e)}), 500

@app.route('/users/<int:id>', methods=['PUT'])
def update_user(id):
    user = db.session.get(User, id)
    if not user: return jsonify({"success": False, "message": "User not found"}), 404
    data = request.json
    try:
        for key, value in data.items():
            if key in ['productCommissions', 'productResellerPrices']: setattr(user, key, json.dumps(value))
            elif hasattr(user, key) and key != 'id': setattr(user, key, value)
        db.session.commit()
        return jsonify({"success": True})
    except Exception as e:
        db.session.rollback()
        return jsonify({"success": False, "message": str(e)}), 500

@app.route('/users/<int:id>', methods=['DELETE'])
def delete_user(id):
    user = db.session.get(User, id)
    if not user or user.name == "Kaparen": return jsonify({"success": False}), 403
    Transaction.query.filter_by(userId=id).delete()
    Payment.query.filter_by(userId=id).delete()
    db.session.delete(user)
    db.session.commit()
    return jsonify({"success": True})

@app.route('/products', methods=['GET'])
def get_products(): return jsonify([p.to_dict() for p in Product.query.all()])

@app.route('/products', methods=['POST'])
def sync_product():
    data = request.json
    prod = db.session.get(Product, data['id'])
    if not prod:
        prod = Product(id=data['id'])
        db.session.add(prod)
    prod.name, prod.quantity, prod.salesPrice, prod.resellerPrice = data['name'], data['quantity'], data['salesPrice'], data['resellerPrice']
    prod.unit, prod.unitCost, prod.lowStockThreshold = data.get('unit', 'g'), data.get('unitCost', 0.0), data.get('lowStockThreshold', 500)
    prod.bulkPrices = json.dumps(data.get('bulkPrices', []))
    db.session.commit()
    return jsonify({"success": True})

@app.route('/transactions', methods=['POST'])
def sync_transaction():
    data = request.json
    if data.get('paymentMethod') in ['Account', 'Konto']:
        user = db.session.get(User, data['userId'])
        if user:
            if data.get('type') == 'EXPENSE': user.balance -= data['amount']
            else: user.balance += data['amount']
    trans = Transaction(userId=data['userId'], productId=data.get('productId', 0), amount=data['amount'], vatAmount=data.get('vatAmount', 0.0), vatRate=data.get('vatRate', 0.0), quantity=data.get('quantity', 0), timestamp=data.get('timestamp', int(time.time() * 1000)), category=data.get('category'), type=data.get('type'), paymentMethod=data.get('paymentMethod'), description=data.get('description'))
    db.session.add(trans)
    db.session.commit()
    return jsonify({"success": True})

@app.route('/admin/trigger-deploy', methods=['POST'])
def trigger_deploy():
    load_dotenv(override=True)
    current_token = os.environ.get('GITHUB_TOKEN')
    if not current_token: return jsonify({"success": False, "message": "Saknar GITHUB_TOKEN"}), 500
        
    data = request.json
    logger.info(f">>> STARTAR TOTAL PUSH (Token: {current_token[-4:]}) <<<")
    
    try:
        # 1. Konfigurera Git för att undvika stopp
        subprocess.run(["git", "config", "user.name", "Kaparen-Server"], check=True)
        subprocess.run(["git", "config", "user.email", "server@kksales.com"], check=True)

        # 2. Pusha kodändringar
        logger.info("Pushar kod till GitHub...")
        subprocess.run(["git", "add", "."], check=True)
        # Commit tillåts misslyckas om inget ändrats
        subprocess.run(["git", "commit", "-m", f"App Update: {data.get('note', 'Auto')}"], capture_output=True)
        # Hämta senaste för att undvika konflikt
        subprocess.run(["git", "pull", "origin", "main", "--rebase"], check=True)
        subprocess.run(["git", "push", "origin", "main"], check=True)

        # 3. Trigga GitHub Action med exakta headers för Actions API
        headers = {
            "Authorization": f"token {current_token}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "KKSales-App-Server"
        }
        payload = {
            "ref": "main",
            "inputs": {
                "version_note": data.get("note", "Update from App"),
                "design_changes": json.dumps(data.get("changes", {}))
            }
        }
        url = f"https://api.github.com/repos/{GITHUB_REPO}/actions/workflows/{GITHUB_WORKFLOW_FILE}/dispatches"
        
        logger.info(f"Triggar workflow: {GITHUB_WORKFLOW_FILE}...")
        res = requests.post(url, headers=headers, json=payload)
        
        if res.status_code == 204:
            logger.info("SUCCESS: GitHub Action triggad! APK bygget påbörjat.")
            return jsonify({"success": True, "message": "Kod puschad och bygge startat!"})
        elif res.status_code == 403:
            logger.error("FEL 403: Token saknar behörighet. Gå till GitHub -> Tokens -> Kryssa i 'workflow'!")
            return jsonify({"success": False, "message": "Token saknar 'workflow' behörighet"}), 403
        else:
            logger.error(f"GitHub API Fel {res.status_code}: {res.text}")
            return jsonify({"success": False, "message": f"API Fel: {res.status_code}"}), res.status_code
            
    except Exception as e:
        logger.exception("KRITISKT FEL:")
        return jsonify({"success": False, "message": str(e)}), 500

@app.route('/stats/economic-overview', methods=['GET'])
def get_economic_overview():
    transactions = Transaction.query.all()
    income = sum(t.amount for t in transactions if t.type == 'INCOME' and t.category == 'SALES')
    costs = 0.0
    for t in transactions:
        if t.category == 'SALES' and t.productId > 0:
            p = db.session.get(Product, t.productId)
            if p: costs += (p.unitCost * t.quantity)
    return jsonify({"income": income, "profit": income - costs, "expenses": sum(t.amount for t in transactions if t.type == 'EXPENSE'), "costs": costs})

if __name__ == '__main__':
    from waitress import serve
    with app.app_context(): db.create_all()
    logger.info("K&K 'Full Access' Server Online på port 8080")
    serve(app, host='0.0.0.0', port=8080)
