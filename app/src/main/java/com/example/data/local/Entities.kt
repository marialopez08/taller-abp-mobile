package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class ClientEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val fullName: String,
  val phoneNumber: String,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "pets",
  foreignKeys = [
    ForeignKey(
      entity = ClientEntity::class,
      parentColumns = ["id"],
      childColumns = ["clientId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index(value = ["clientId"])],
)
data class PetEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val clientId: Long,
  val name: String,
  val breed: String,
  val sizeCode: String,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "services",
  indices = [Index(value = ["code"], unique = true)],
)
data class ServiceEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val code: String,
  val name: String,
  val description: String,
  val basePriceCop: Int,
)

@Entity(
  tableName = "service_prices",
  foreignKeys = [
    ForeignKey(
      entity = ServiceEntity::class,
      parentColumns = ["id"],
      childColumns = ["serviceId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index(value = ["serviceId", "dogSizeCode"], unique = true)],
)
data class ServicePriceEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val serviceId: Long,
  val dogSizeCode: String,
  val priceCop: Int,
)

@Entity(
  tableName = "appointments",
  indices = [
    Index(value = ["dateIso", "timeSlotId"]),
    Index(value = ["statusCode"]),
  ],
)
data class AppointmentEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val clientId: Long,
  val petId: Long,
  val serviceId: Long,
  val clientName: String,
  val clientPhone: String,
  val petName: String,
  val petBreed: String,
  val serviceCode: String,
  val serviceName: String,
  val dogSizeCode: String,
  val dateIso: String,
  val timeSlotId: String,
  val timeSlotLabel: String,
  val priceCop: Int,
  val statusCode: String,
  val createdAt: Long = System.currentTimeMillis(),
)
