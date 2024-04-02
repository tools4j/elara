/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.plugin.activation;

import org.tools4j.elara.plugin.api.SystemPlugin;

import static java.util.Objects.requireNonNull;

public class ActivationPlugin implements SystemPlugin<ActivationState> {

    private final ActivationConfig config;
    private final ActivationPluginSpecification specification = new ActivationPluginSpecification(this);
    private ActivationState activationState;

    public ActivationPlugin(final ActivationConfig config) {
        this.config = requireNonNull(config);
    }

    public boolean isActive() {
        return activationState != null && activationState.active();
    }

    public ActivationConfig config() {
        return config;
    }

    public static ActivationContext configure() {
        return ActivationContext.create();
    }

    public boolean activate() {
        if (activationState == null) {
            return false;
        }
        activationState.active(true);
        return true;
    }

    public void deactivate() {
        if (activationState == null) {
            return;
        }
        activationState.active(false);
    }

    @Override
    public SystemPluginSpecification<ActivationState> specification() {
        return specification;
    }

    void init(final ActivationState activationState) {
        this.activationState = requireNonNull(activationState);
    }

}
