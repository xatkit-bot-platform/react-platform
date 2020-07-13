package com.xatkit.plugins.react.platform.action;

import com.xatkit.core.platform.action.RuntimeMessageAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.react.platform.ReactPlatform;
import com.xatkit.plugins.react.platform.socket.action.SendBotMessage;
import com.xatkit.plugins.react.platform.socket.action.QuickButtonDescriptor;
import com.xatkit.plugins.react.platform.socket.action.SetMessageLoaderObject;
import com.xatkit.plugins.react.platform.socket.SocketEventTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * A {@link RuntimeMessageAction} that posts a {@code message} to a given xatkit-react {@code channel}.
 */
public class PostMessage extends RuntimeMessageAction<ReactPlatform> {

    /**
     * The descriptors of the <i>quick buttons</i> to print to the user.
     */
    private List<QuickButtonDescriptor> quickButtonDescriptors;

    /**
     * The channel to post the message to.
     */
    private String channel;

    /**
     * Constructs a new {@link PostMessage} with the provided {@code runtimePlatform}, {@code session}, {@code
     * message}, and {@code channel}.
     * <p>
     * This constructor is similar to {@code new PostMessage(runtimePlatform, session, message, Collections.emptyList
     * (), channel)}.
     *
     * @param runtimePlatform the {@link ReactPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param message         the message to post
     * @param channel         the xatkit-react channel to post the message to
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code message} or {@code channel} is {@code null}
     */
    public PostMessage(ReactPlatform runtimePlatform, XatkitSession session, String message, String channel) {
        this(runtimePlatform, session, message, Collections.emptyList(), channel);
    }

    /**
     * Constructs a new {@link PostMessage} with the provided {@code runtimePlatform}, {@code session}, {@code
     * message}, {@code buttons}, and {@code channel}.
     *
     * @param runtimePlatform the {@link ReactPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param message         the message to post
     * @param buttons         the quick message buttons to display with the message
     * @param channel         the xatkit-react channel to post the message to
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code message} or {@code channel} is {@code null}
     */
    public PostMessage(ReactPlatform runtimePlatform, XatkitSession session, String message,
                       List<String> buttons, String channel) {
        super(runtimePlatform, session, message);
        checkArgument(nonNull(channel) && !(channel.isEmpty()), "Cannot construct a %s action with the provided " +
                "channel %s, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        checkNotNull(buttons, "Cannot build a %s from the provided buttons %s", this.getClass().getSimpleName(),
                buttons);
        this.channel = channel;
        this.quickButtonDescriptors = new ArrayList<>();
        buttons.forEach(label -> this.quickButtonDescriptors.add(new QuickButtonDescriptor(label, label)));
    }

    /**
     * Notifies the client that the message is delayed.
     * <p>
     * This method allows to print loading dots on the client side while the action is delayed. If the provided
     * {@code delayValue == 0} no notification is sent.
     *
     * @param delayValue the value of the delay (in ms)
     */
    @Override
    protected void beforeDelay(int delayValue) {
        if (delayValue > 0) {
            this.runtimePlatform.getSocketIOServer().getClient(UUID.fromString(channel))
                    .sendEvent(SocketEventTypes.SET_MESSAGE_LOADER.label, new SetMessageLoaderObject(true));
        }
    }

    /**
     * Posts the provided {@code message} to the given {@code channel}.
     * <p>
     * Posted messages are pushed to the client application using the underlying socket server.
     *
     * @return {@code null}
     */
    @Override
    protected Object compute() {
        this.runtimePlatform.getSocketIOServer().getClient(UUID.fromString(channel))
                .sendEvent(SocketEventTypes.BOT_MESSAGE.label, new SendBotMessage("xatkit", message,
                        this.quickButtonDescriptors));
        return null;
    }

    @Override
    protected XatkitSession getClientSession() {
        return this.runtimePlatform.getSessionForSocketId(channel);
    }
}
