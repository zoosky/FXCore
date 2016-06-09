package com.miriamlaurel.fxcore.market

import java.time.Instant

import com.miriamlaurel.fxcore._
import com.miriamlaurel.fxcore.asset.{AssetClass, Currency}
import com.miriamlaurel.fxcore.instrument.Instrument

case class Market(books: Map[Instrument, OrderBook], pivot: Currency = USD, timestamp: Instant = Instant.EPOCH) {

  def apply(instrument: Instrument): OrderBook = books(instrument)

  def get(instrument: Instrument): Option[OrderBook] = books.get(instrument)

  def put(snapshot: OrderBook): Market = copy(books = books + (snapshot.instrument -> snapshot), timestamp = snapshot.timestamp)

  def put(op: OrderOp): Market = {
    val oldBook = this.get(op.instrument) match {
      case Some(book) ⇒ book
      case None ⇒ OrderBook.empty(op.instrument)
    }
    val newBook = oldBook.apply(op)
    this.put(newBook)
  }


  def quote(instrument: Instrument, amount: BigDecimal = 0): Option[Quote] = {
    if (instrument.base == instrument.counter)
      Some(Quote(instrument, Some(1), Some(1), timestamp))
    else if (books.contains(instrument)) Some(apply(instrument).quote(amount))
    else if (books.contains(instrument.reverse)) Some(apply(instrument.reverse).quote(amount).reverse)
    else for {
      a <- quoteToPivot(instrument.base)
      b <- quoteToPivot(instrument.counter)
      x <- Some(if (isReverse(a.instrument)) a.reverse else a)
      y <- Some(if (isStraight(b.instrument)) b.reverse else b)
      aBid <- x.bid
      bBid <- y.bid
      aAsk <- x.ask
      bAsk <- y.ask
      bid <- Some(aBid * bBid)
      ask <- Some(aAsk * bAsk)
      timestamp = Instant.ofEpochMilli(a.timestamp.toEpochMilli max b.timestamp.toEpochMilli)
    } yield Quote(instrument, Some(bid), Some(ask), timestamp)
  }

  def convert(from: Money,
              to: AssetClass,
              side: QuoteSide.Value,
              amount: BigDecimal = 0): Option[Money] = from match {
    case Zilch ⇒ Some(Zilch)
    case m: Monetary ⇒ for (q <- quote(Instrument(m.asset, to), amount);
                            p <- q.apply(side)) yield Money(p * m.amount, to)
  }

  private def quoteToPivot(asset: AssetClass): Option[Quote] = {
    val straight = Instrument(asset, pivot)
    val reverse = Instrument(pivot, asset)
    if (get(straight).isDefined) Some(apply(straight).best)
    else if (get(reverse).isDefined) Some(apply(reverse).best) else None
  }

  private def isStraight(instrument: Instrument) = pivot == instrument.counter

  private def isReverse(instrument: Instrument) = pivot == instrument.base
}

object Market {
  def apply(snapshots: OrderBook*): Market = Market(snapshots.map(s ⇒ s.instrument -> s).toMap)
}
