dart
// lib/models/sale.dart
  class Sale {
String id;
String userId;
String productId;
int amount;
double unitPrice;
DateTime date;
double total;
Sale({
required this.id,
required this.userId,
required this.productId,
required this.amount,
required this.unitPrice,
DateTime? date,
}) : this.date = date ?? DateTime.now(),
this.total = amount * unitPrice;
Map<String, dynamic> toMap() => {
'id': id,
'userId': userId,
'productId': productId,
'amount': amount,
'unitPrice': unitPrice,
'date': date.toIso8601String(),
'total': total,
};
factory Sale.fromMap(Map<String, dynamic> map) => Sale(
id: map['id'],
userId: map['userId'],
productId: map['productId'],
amount: map['amount'],
unitPrice: (map['unitPrice'] as num).toDouble(),
date: DateTime.parse(map['date']),
);
factory Sale.fromJson(Map<String, dynamic> json) => Sale.fromMap(json);
Map<String, dynamic> toJson() => toMap();
}
