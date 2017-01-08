package eu.bcvsolutions.idm.notification.service.impl;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class NotificationRouteBuilder extends RouteBuilder {
	
	@Override
    public void configure() throws Exception {		
		// suports email only for now, @see http://redmine.czechidm.com/issues/65
		// TODO: complex routing - by topic and identity configuration
    	from("direct:notifications")
    		.choice()
    			.when().simple("${body.topic} == 'idm:websocket'")
    				.to("bean:websocketService?method=send")
    			.otherwise() // TODO: this branch will be removed
		    		.multicast() // TODO: .parallelProcessing()
		    		.to("bean:emailService?method=send", "bean:consoleNotificationService?method=send");
		    		// .bean(Object.class);
    	//
    	// register email sender
    	from("direct:emails").to("bean:emailer?method=send");
    }
}
