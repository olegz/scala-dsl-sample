/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spring.integration.scala.dsl

import org.junit.Test
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.SpelParserConfiguration
import org.springframework.integration.dsl.utils.DslUtils
import org.springframework.integration.Message
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.core.MessageCreator
import javax.jms.Session
import javax.jms.TextMessage
import org.springframework.integration.dsl._

/**
 * @author Oleg Zhurakousky
 */
class DSLUsageDemoTests {

  @Test
  def demoSend = {
   
    val messageFlow =
      transform.using { m: Message[String] => m.getPayload().toUpperCase() } -->
      handle.using { m: Message[_] => println(m) }

    messageFlow.send("hello")
    println("done")
  }
  
  @Test
  def demoSendWithExplicitDirectChannel = {
    val messageFlow =
      Channel("direct") -->
      transform.using { m: Message[String] => m.getPayload().toUpperCase() } -->
      handle.using { m: Message[_] => println(m) }

    messageFlow.send("hello")
    println("done")
  }
  
  @Test
  def demoSendWithExplicitPubSubChannelOneSubscriber = {
    val messageFlow =
      PubSubChannel("direct") -->
      transform.using { m: Message[String] => m.getPayload().toUpperCase() } -->
      handle.using { m: Message[_] => println(m) }

    messageFlow.send("hello")
    println("done")
  }

  @Test
  def demoSendAndReceive = {
    val messageFlow =
      transform.using { m: Message[String] => m.getPayload().toUpperCase() } -->
      handle.using { m: Message[_] => println(m); m }

    val reply = messageFlow.sendAndReceive[String]("hello")
    println(reply)
  }

  @Test
  def demoSendWithExplicitPubSubChannelMultipleSubscriber = {
    val messageFlow =
      transform.using { m: Message[String] => m.getPayload().toUpperCase() }.where(name="myTransformer") -->
      PubSubChannel("pubSub") --> (
          transform.using { m: Message[_] => m.getPayload() + " - subscriber-1" } -->
          handle.using { m: Message[_] => println(m) }
          ,
          transform.using { m: Message[_] => m.getPayload() + " - subscriber-2" } -->
          handle.using { m: Message[_] => println(m) }
      )
    
    messageFlow.send("hello")
    println("done")
  }

  @Test
  def demoSendWithMessagingBridge = {
    val messageFlow =
      Channel("A") -->
      Channel("B") -->
      handle.using { m: Message[_] => println("From Hello channel - " + m) }

    messageFlow.send("hello")

    println("done")
  }

  @Test
  def demoSendWithPolingMessagingBridge = {
    val messageFlow =
      Channel("A") -->
      Channel("B").withQueue --> poll.usingFixedRate(1) -->
      handle.using { m: Message[_] => println("From Hello channel - " + m) }

    messageFlow.send("hello")
    Thread.sleep(1000)
    println("done")
  }

  @Test
  def demoSendWithHeaderEnricherWithTuple = {
    val enricherA = 
      enrich.header("hello" -> "bye") --> 
      handle.using { m: Message[_] => println(m) }
      
    enricherA.send("Hello")
    println("done")
  }

  @Test
  def demoSendWithHeaderEnricherWithFunctionAsValue = {
    val enricherB = 
      enrich.header("hello" -> Some({ m: Message[String] => m.getPayload().toUpperCase() })) --> 
      handle.using { m: Message[_] => println(m) }
      
    enricherB.send("Hello")
    println("done")
  }

  @Test
  def demoSendWithHeaderEnricherWithMessageFunctionAsProcessor = {
    val enricherB = 
      enrich.header("hello" -> { m: Message[String] => m.getPayload().toUpperCase() }) --> 
      handle.using { m: Message[_] => println(m) }
      
    enricherB.send("Hello")
    println("done")
  }

  @Test
  def demoSendWithHeaderEnricherWithStaticValue = {
    val enricher = 
      enrich.header("hello" -> "foo") --> 
      handle.using { m: Message[_] => println(m) }
      
    enricher.send("Hello")
    println("done")
  }

  @Test
  def demoSendWithHeaderEnricherWithSpELExpression = {
    val expression = new SpelExpressionParser(new SpelParserConfiguration(true, true)).parseExpression("(2 * 6) + ' days of Christmas'");
    val enricherB = 
      enrich.header("phrase" -> expression) --> 
      handle.using { m: Message[_] => println(m) }
      
    enricherB.send("Hello")
    println("done")
  }

  @Test
  def demoSendWithHeaderEnricherWithMultiTuple = {
    val expression = new SpelExpressionParser(new SpelParserConfiguration(true, true)).parseExpression("(2 * 6) + ' days of Christmas'");
    val enricher =
      enrich.headers("foo" -> "foo",
        "bar" -> { m: Message[String] => m.getPayload().toUpperCase() },
        "phrase" -> expression) -->
        handle.using { m: Message[_] => println(m) }

    enricher.send("Hello")
    println("done")
  }

  @Test
  def demoSendWithContentEnricher = {
    val employee = new Employee("John", "Doe", 23)
    val enricher =
      enrich { p: Person => p.name = employee.firstName + " " + employee.lastName; p.age = employee.age; p } -->
      handle.using { m: Message[_] => println(m) }

    enricher.send(new Person)
    println("done")
  }

  @Test
  def demoSendWithContentEnricherWithSubFlow = {

    val employeeBuldingFlow =
      transform.using { attributes: List[String] => new Employee(attributes(0), attributes(1), Integer.parseInt(attributes(2))) }

    val enricher =
      enrich { p: Person =>
        val employee = employeeBuldingFlow.sendAndReceive[Employee](List[String]("John", "Doe", "25"))
        p.name = employee.firstName + " " + employee.lastName
        p.age = employee.age
        p
      } -->
      handle.using { m: Message[_] => println(m) }

    enricher.send(new Person)
    println("done")
  }

  case class Person(var name: String = null, var age: Int = 0)

  class Employee(val firstName: String, val lastName: String, val age: Int)
  
  @Test
  def demoSendWithPayloadTypeRouter = {
    val routedFlow =  
      handle.using{m:Message[_] => println("Payload is of type: " + m.getPayload().getClass()); m} -->
      route.onPayloadType(
        when(classOf[String]) then 
      	  handle.using{value:String => println("String type: " + value)}
        ,	
        when(classOf[Int]) then 
          handle.using{value:Int => println("Int type: " + value)}
      ).where(name = "myRouter")
    
    routedFlow.send("Spring Integration")
    
    routedFlow.send(25)
    println("done")
  }
  
  def demoSendWithHeaderValueRouter = {
    val routedFlow =  
      handle.using{m:Message[_] => println("Payload is of type: " + m.getPayload().getClass()); m} -->
      route.onValueOfHeader("routingHeader")(
        when("foo") then 
      	  handle.using{value:String => println("String type: " + value)}
        ,	
        when("bar") then 
          handle.using{value:Int => println("Int type: " + value)}

      ).where(name = "myRouter")
    
    routedFlow.send("Spring Integration", headers = Map("routingHeader" -> "foo"))
    
    routedFlow.send("Spring Integration", headers = Map("routingHeader" -> "bar"))
    
    routedFlow.send(25)
    println("done")
  }

  @Test
  def demoSendWithHttpOutboundGatewayWithFunctionUrl = {

    val tickerService =
      transform.using { s: String =>
        s.toLowerCase() match {
          case "vmw" => "VMWare"
          case "orcl" => "Oracle"
          case _ => "vmw"
        }
      }

    val httpFlow = 
            enrich.header("company" -> {name:String => tickerService.sendAndReceive[String](name)}) -->
    		http.GET[String] { m: Message[String] => "http://www.google.com/finance/info?q=" + m.getPayload()} -->
    		handle.using{quotes:Message[_] => println("QUOTES for " + quotes.getHeaders().get("company") + " : " + quotes)}
    		
    httpFlow.send("vmw")
     
    println("done")
  }
  
  @Test
  def demoSendWithHttpOutboundGatewayWithStringUrl = {

    val tickerService =
      transform.using { s: String =>
        s.toLowerCase() match {
          case "vmware" => "vmw"
          case "oracle" => "orcl"
          case _ => ""
        }
      }

    val httpFlow = 
    		http.GET[String]("http://www.google.com/finance/info?q=" + tickerService.sendAndReceive[String]("Oracle")) -->
    		handle.using{quotes:Message[_] => println("QUOTES for " + quotes.getHeaders().get("company") + " : " + quotes)}
    		
    httpFlow.send("static")
    
    println("done")
  }
  
  @Test
  def demoSendWithHttpOutboundGatewayWithPOSTthenGET = {

    val httpFlow = 
    		http.POST[String]("http://posttestserver.com/post.php") -->
            transform.using{response:String => println(response) // poor man transformer to extract URL from which the POST results are visible
              response.substring(response.indexOf("View") + 11, response.indexOf("Post") - 1)} -->
            http.GET[String]{url:String => url} -->
    		handle.using{response:String => println(response)}
    				
    httpFlow.send("Spring Integration")
    
    println("done")
  }
  
  @Test
  def demoListenOnJmsInboundGateway  = {
    val connectionFactory = JmsDslTestUtils.localConnectionFactory
    
    val flow = 
      jms.listen(requestDestinationName = "myQueue", connectionFactory = connectionFactory) -->
      handle.using{m:Message[_] => println("logging existing message and passing through " + m); m} -->
      transform.using{value:String => value.toUpperCase()}
    
    flow.start  
    
    val jmsTemplate = new JmsTemplate(connectionFactory);
    val request = new org.apache.activemq.command.ActiveMQQueue("myQueue")
    val reply = new org.apache.activemq.command.ActiveMQQueue("myReply")
    jmsTemplate.send(request, new MessageCreator {
		def  createMessage(session:Session) = {
			val message = session.createTextMessage();
			message.setText("Hello from JMS");
			message.setJMSReplyTo(reply);
			message;
		}
	});
    
    val replyMessage = jmsTemplate.receive(reply);
    println("Reply Message: " + replyMessage.asInstanceOf[TextMessage].getText())
   
    flow.stop
    println("done")
  }
}