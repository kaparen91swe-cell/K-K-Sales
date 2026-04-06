dart
// lib/models/product.dart
  class Product {
String id;
String name;
double price;
double? cost;
Product({required this.id, required this.name, required this.price, this.cost});
Map<String, dynamic> toMap() => {
'id': id,
'name': name,
'price': price,
'cost': cost,
};
factory Product.fromMap(Map<String, dynamic> map) => Product(
id: map['id'],
name: map['name'],
price: (map['price'] as num).toDouble(),
cost: map['cost'] != null ? (map['cost'] as num).toDouble() : null,
);
factory Product.fromJson(Map<String, dynamic> json) => Product.fromMap(json);
Map<String, dynamic> toJson() => toMap();
}
