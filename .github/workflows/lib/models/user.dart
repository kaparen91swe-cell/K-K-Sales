`dart
// lib/models/user.dart
class User {
String id;
String name;
double balance;
DateTime created;
String role; // 'admin' eller 'user'
User({required this.id, required this.name, this.balance = 0, DateTime? created, this.role = 'user'})
: this.created = created ?? DateTime.now();
Map<String, dynamic> toMap() => {
'id': id,
'name': name,
'balance': balance,
'created': created.toIso8601String(),
'role': role,
};
factory User.fromMap(Map<String, dynamic> map) => User(
id: map['id'],
name: map['name'],
balance: (map['balance'] as num).toDouble(),
created: DateTime.parse(map['created']),
role: map['role'] ?? 'user',
);
factory User.fromJson(Map<String, dynamic> json) => User.fromMap(json);
Map<String, dynamic> toJson() => toMap();
}
