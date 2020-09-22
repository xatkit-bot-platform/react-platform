package com.xatkit.plugins.react.platform.io;

import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import com.xatkit.intent.IntentFactory;
import com.xatkit.plugins.chat.ChatUtils;
import com.xatkit.plugins.react.platform.ReactPlatform;
import com.xatkit.plugins.react.platform.socket.SocketEventTypes;
import com.xatkit.plugins.react.platform.socket.action.InitConfirm;
import com.xatkit.plugins.react.platform.socket.event.Init;
import com.xatkit.plugins.react.platform.utils.ReactUtils;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import static com.xatkit.dsl.DSL.event;
import static java.util.Objects.isNull;

/**
 * A {@link RuntimeEventProvider} that fires non-textual events related to the Xatkit react chat component.
 * <p>
 * This class fires component life-cycle events, e.g. when the chat component is ready or when it disconnects from
 * the socket server.
 */
public class ReactEventProvider extends RuntimeEventProvider<ReactPlatform> {

    /**
     * Constructs a {@link ReactEventProvider} and binds it to the provided {@code reactPlatform}.
     *
     * @param reactPlatform the {@link ReactPlatform} managing this provider
     */
    public ReactEventProvider(ReactPlatform reactPlatform) {
        super(reactPlatform);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method registers the listeners to the socker server to receive widget life-cycle notifications (client
     * connection/disconnection, etc) and creates the corresponding {@link EventInstance}s. Received notifications
     * won't be processed until this method is invoked.
     *
     * @see #ClientReady
     * @see #ClientClosed
     */
    @Override
    public void start(Configuration configuration) {
        super.start(configuration);
        /*
         * Register the listener that creates the Client_Ready event.
         * This event is fired every time the client connects to the socket server.
         */
        this.runtimePlatform.getSocketIOServer().removeAllListeners(SocketEventTypes.INIT.label);
        this.runtimePlatform.getSocketIOServer().addEventListener(SocketEventTypes.INIT.label, Init.class,
                (socketIOClient, initObject, ackRequest) -> {
                    String socketId = socketIOClient.getSessionId().toString();

                    StateContext context = this.runtimePlatform.getStateContextForSocketId(socketId);

                    if (isNull(context)) {
                        String conversationId = initObject.getConversationId();
                        Log.debug("Client requested conversation {0}", conversationId);
                        context = this.runtimePlatform.createStateContextForConversation(socketId, conversationId);
                        context.setOrigin(initObject.getOrigin());
                        socketIOClient.sendEvent(SocketEventTypes.INIT_CONFIRM.label,
                                new InitConfirm(context.getContextId()));
                    }
                    /*
                     * The session already exists, no need to send an ack event.
                     */

                    ContextParameter chatChannelParameter =
                            ClientReady.getParameter(ChatUtils.CHAT_CHANNEL_CONTEXT_KEY);
                    ContextParameter reactHostnameParameter =
                            ClientReady.getParameter(ReactUtils.REACT_HOSTNAME_CONTEXT_KEY);
                    ContextParameter reactUrlParameter =
                            ClientReady.getParameter(ReactUtils.REACT_URL_CONTEXT_KEY);
                    ContextParameter reactOriginParameter =
                            ClientReady.getParameter(ReactUtils.REACT_ORIGIN_CONTEXT_KEY);


                    EventInstance eventInstance = IntentFactory.eINSTANCE.createEventInstance();
                    eventInstance.setDefinition(ClientReady);
                    ContextParameterValue chatChannelValue = IntentFactory.eINSTANCE.createContextParameterValue();
                    chatChannelValue.setContextParameter(chatChannelParameter);
                    chatChannelValue.setValue(socketId);
                    eventInstance.getValues().add(chatChannelValue);
                    ContextParameterValue reactHostnameValue = IntentFactory.eINSTANCE.createContextParameterValue();
                    reactHostnameValue.setContextParameter(reactHostnameParameter);
                    reactHostnameValue.setValue(initObject.getHostname());
                    eventInstance.getValues().add(reactHostnameValue);
                    ContextParameterValue reactUrlValue = IntentFactory.eINSTANCE.createContextParameterValue();
                    reactUrlValue.setContextParameter(reactUrlParameter);
                    reactUrlValue.setValue(initObject.getUrl());
                    eventInstance.getValues().add(reactUrlValue);
                    ContextParameterValue reactOriginValue = IntentFactory.eINSTANCE.createContextParameterValue();
                    reactOriginValue.setContextParameter(reactOriginParameter);
                    reactOriginValue.setValue(initObject.getOrigin());
                    eventInstance.getValues().add(reactOriginValue);

                    this.sendEventInstance(eventInstance, context);

                });
        /*
         * Register the listener that creates the Client_Closed event.
         * This event is fired every time the client disconnects from the socket server.
         */
        this.runtimePlatform.getSocketIOServer().addDisconnectListener(socketIOClient -> {
            String channel = socketIOClient.getSessionId().toString();
            StateContext context = this.runtimePlatform.getStateContextForSocketId(channel);

            ContextParameter chatChannelParameter = ClientClosed.getParameter(ChatUtils.CHAT_CHANNEL_CONTEXT_KEY);

            EventInstance eventInstance = IntentFactory.eINSTANCE.createEventInstance();
            eventInstance.setDefinition(ClientClosed);
            ContextParameterValue chatChannelValue = IntentFactory.eINSTANCE.createContextParameterValue();
            chatChannelValue.setContextParameter(chatChannelParameter);
            chatChannelValue.setValue(channel);
            eventInstance.getValues().add(chatChannelValue);

            this.sendEventInstance(eventInstance, context);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        /*
         * Do nothing, the socket server is started asynchronously.
         */
    }

    /**
     * The {@link EventDefinition} that is fired when a client connects to the widget.
     */
    public static EventDefinition ClientReady = event("Client_Ready")
            .parameter(ChatUtils.CHAT_CHANNEL_CONTEXT_KEY)
            .parameter(ReactUtils.REACT_HOSTNAME_CONTEXT_KEY)
            .parameter(ReactUtils.REACT_URL_CONTEXT_KEY)
            .parameter(ReactUtils.REACT_ORIGIN_CONTEXT_KEY)
            .getEventDefinition();

    /**
     * The {@link EventDefinition} that is fired when a client connection is closed.
     */
    public static EventDefinition ClientClosed = event("Client_Closed")
            .parameter(ChatUtils.CHAT_CHANNEL_CONTEXT_KEY)
            .getEventDefinition();
}
