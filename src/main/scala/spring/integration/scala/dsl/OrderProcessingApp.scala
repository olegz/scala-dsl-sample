package spring.integration.scala.dsl
import org.springframework.integration.dsl.implicits._
import org.springframework.integration.dsl.builders._
import org.springframework.integration.Message
import java.util.concurrent.Executors

/**
 * OrderProcessing sample
 * 
 * @author Oleg Zhurakousky
 */
object OrderProcessingApp extends Application {

  val validOrder = PurchaseOrder(List(
    PurchaseOrderItem("books", "Spring Integration in Action"),
    PurchaseOrderItem("books", "DSLs in Action"),
    PurchaseOrderItem("bikes", "Canyon Torque FRX")))

  val invalidOrder = PurchaseOrder(List())

  val errorFlow = handle.using { m: Message[_] => println("Received ERROR: " + m); "ERROR processing order" }
 
  val aggregationFlow = aggregate()

  val bikeFlow =
    handle.using { m: Message[_] => println("Processing bikes order: " + m); m } -->
    aggregationFlow

  val orderProcessingFlow =
      filter.using { p: PurchaseOrder => !p.items.isEmpty }.where(exceptionOnRejection = true) -->
      split.using { p: PurchaseOrder => p.items } -->
      Channel.withDispatcher(taskExecutor = Executors.newCachedThreadPool) -->
      route.using { pi: PurchaseOrderItem => pi.itemType }(
        when("books") then
          handle.using { m: Message[_] => println("Processing books order: " + m); m } -->
          aggregationFlow,
        when("bikes") then
          bikeFlow
      )

  val result = orderProcessingFlow.sendAndReceive[Any](validOrder, errorFlow = errorFlow)
  
  /*
   * Un-comment the following line to see how 'invalidOrder' is processed. Invalid order will raise
   * an exception and the flow will be re-routed to the 'errorFlow'
   */
  
//  val result = orderProcessingFlow.sendAndReceive[Any](invalidOrder, errorFlow = eFlow)

  println("Result: " + result)
}


case class PurchaseOrder(val items: List[PurchaseOrderItem])

case class PurchaseOrderItem(val itemType: String, val title: String)