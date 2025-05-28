// Colección: usuarios
{
  "uid": "abc123", // ID Firebase Auth
  "email": "usuario@test.com",
  "nombre": "Juan Pérez",
  "telefono": "+56912345678",
  "vehiculosFavoritos": ["vehiculo1", "vehiculo2"] // Referencias
}

// Colección: vehiculos
{
  "id": "vehiculo1",
  "marca": "Toyota",
  "modelo": "Corolla",
  "año": 2023,
  "precioDia": 45.99,
  "disponible": true,
  "fotosURLs": ["gs://bucket/foto1.jpg"]
}

// Colección: reservas
{
  "id": "reserva789",
  "usuarioId": "abc123", // Relación
  "vehiculoId": "vehiculo1", // Relación
  "fechaInicio": "2024-10-05T10:00:00Z",
  "fechaFin": "2024-10-10T10:00:00Z",
  "estado": "confirmada", // confirmada/cancelada/finalizada
  "precioTotal": 275.94,
  "servicios": ["servicio1", "servicio2"] // Referencias
}

// Colección: servicios
{
  "id": "servicio1",
  "nombre": "Seguro Full",
  "descripcion": "Cobertura total",
  "precio": 30.00
}