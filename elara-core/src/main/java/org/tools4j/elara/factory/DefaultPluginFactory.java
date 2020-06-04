/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.tools4j.elara.factory;

import org.tools4j.elara.init.Context;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.base.BasePlugin;
import org.tools4j.elara.plugin.base.BaseState;

import static java.util.Objects.requireNonNull;

public class DefaultPluginFactory implements PluginFactory {

    private static final Plugin.Context[] EMPTY_PLUGIN_CONTEXTS = {};

    private final ElaraFactory elaraFactory;

    public DefaultPluginFactory(final ElaraFactory elaraFactory) {
        this.elaraFactory = requireNonNull(elaraFactory);
    }

    protected ElaraFactory elaraFactory() {
        return elaraFactory;
    }

    protected Context context() {
        return elaraFactory.context();
    }

    @Override
    public Plugin.Context[] plugins() {
        return context().plugins().pluginContexts().toArray(EMPTY_PLUGIN_CONTEXTS);
    }

    @Override
    public BaseState.Mutable baseState() {
        for (final Plugin.Context plugin : plugins()) {
            if (plugin instanceof BasePlugin.BaseContext) {
                return ((BasePlugin.BaseContext)plugin).baseState();
            }
        }
        return BasePlugin.BaseContext.createDefaultBaseState();
    }
}
