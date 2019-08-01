package com.xatkit.plugins.react.platform;

import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.plugins.chat.ChatUtils;

/**
 * An utility interface that holds xatkit-react related helpers and context keys.
 */
public interface ReactUtils extends ChatUtils {

    /**
     * The {@link RuntimeContexts} key used to store React-related information.
     */
    String REACT_CONTEXT_KEY = "react";
}
