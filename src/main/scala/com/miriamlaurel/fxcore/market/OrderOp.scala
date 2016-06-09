package com.miriamlaurel.fxcore.market

import java.time.Instant
import java.util.UUID

import com.miriamlaurel.fxcore.Timestamp
import com.miriamlaurel.fxcore.instrument.Instrument
import com.miriamlaurel.fxcore.party.Party

sealed trait OrderOp extends Timestamp {
  def instrument: Instrument
  def party: Party
  def id: String
}

case class AddOrder(order: Order, override val timestamp: Instant) extends OrderOp {
  override def instrument: Instrument = order.key.instrument
  override def party: Party = order.key.party
  override def id = order.key.id
}

case class ChangeOrder(newOrder: Order, override val timestamp: Instant) extends OrderOp {
  override def instrument: Instrument = newOrder.key.instrument
  override def party: Party = newOrder.key.party
  override def id = newOrder.key.id
}

case class RemoveOrder(orderKey: OrderKey, override val timestamp: Instant) extends OrderOp {
  override def instrument: Instrument = orderKey.instrument
  override def party: Party = orderKey.party
  override def id = orderKey.id
}

case class ReplaceParty(party: Party, orderBook: OrderBook, override val timestamp: Instant,
                        override val id: String = UUID.randomUUID().toString) extends OrderOp {
  override def instrument: Instrument = orderBook.instrument
}