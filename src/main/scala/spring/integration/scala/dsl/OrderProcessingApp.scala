package spring.integration.scala.dsl
import org.springframework.integration.dsl.implicites._
import org.springframework.integration.dsl.builders._
import org.springframework.integration.Message
import java.util.concurrent.Executors

/**
 * OrderProcessing sample
 * 
 * @author Oleg Zhurakousky
 */
object OrderProcessingApp {
 
  def main(args: Array[String]): Unit = {
    
    val validOrder = PurchaseOrder(List(
    PurchaseOrderItem("books", "Spring Integration in Action"),
    PurchaseOrderItem("books", "DSLs in Action"),
    PurchaseOrderItem("bikes", "Canyon Torque FRX")))

  val invalidOrder = PurchaseOrder(List())

  val errorFlow = handle.using { m: Message[_] => println("Received ERROR: " + m); "ERROR processing order" }
 
  val aggregateOrder = aggregate()

  val processBikeOrder =
    handle.using { m: Message[_] => println("Processing bikes order: " + m); m } -->
    aggregateOrder

  val orderProcessingFlow =
      filter.using{p: PurchaseOrder => !p.items.isEmpty}.where(exceptionOnRejection = true) -->
      split.using { p: PurchaseOrder => p.items } -->
      Channel.withDispatcher(taskExecutor = Executors.newCachedThreadPool) -->
      route.using { pi: PurchaseOrderItem => pi.itemType }(
        when("books") then
          handle.using { m: Message[_] => println("Processing books order: " + m); m } -->
          aggregateOrder,
        when("bikes") then
          processBikeOrder
      )

  val validOrderResult = orderProcessingFlow.sendAndReceive[Any](validOrder, errorFlow = errorFlow)
  println("Result: " + validOrderResult)
 
  val invalidOrderResult = orderProcessingFlow.sendAndReceive[Any](invalidOrder, errorFlow = errorFlow)
  println("Result: " + invalidOrderResult)
    
  }
}


case class PurchaseOrder(val items: List[PurchaseOrderItem])

case class PurchaseOrderItem(val itemType: String, val title: String)