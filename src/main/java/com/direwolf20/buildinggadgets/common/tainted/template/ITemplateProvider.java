package com.direwolf20.buildinggadgets.common.tainted.template;

import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.network.Target;
import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentProvider;

import java.util.UUID;

public interface ITemplateProvider extends Component {
    UUID getId(ITemplateKey key);

    Template getTemplateForKey(ITemplateKey key);

    default <T extends Throwable> Template getTemplateForKey(ComponentProvider provider) throws T {
        return getTemplateForKey(BGComponent.TEMPLATE_KEY_COMPONENT.getNullable(provider));
    }

    /**
     * Overrides the TemplateItem for the given key.
     * <p>
     * Please prefer using TemplateTransactions and only use this if you absolutely have to (f.e. this is used for syncing).
     * This is, because if you use this Method and someone else is running a Transaction on the previous value associated with key
     * the result will silently not show up!
     *
     * @param key      The key for which the TemplateItem should be set
     * @param template The TemplateItem to set
     */
    void setTemplate(ITemplateKey key, Template template);

    /**
     * Requests an update <b>from</b> the other side - aka requests the other side to send an update. Has <b>no effect on Servers</b> as,
     * the Client from which to request the update from would be undefined.
     *
     * @param key The {@link ITemplateKey} for which to request an update
     * @return whether or not an update was requested
     */
    boolean requestUpdate(ITemplateKey key);

    /**
     * Requests an update from the specified target.
     *
     * @param target The target to which to request the update
     * @see #requestUpdate(ITemplateKey)
     */
    boolean requestUpdate(ITemplateKey key, Target target);

    /**
     * Requests an update <b>for<b/> the other side - aka sends an update packet to it. On the client this will send the data to the server,
     * on the server this will send the data to <b>all logged in Clients</b>.
     *
     * @param key The key to request a remote update for
     * @return whether or not a remote update was requested.
     */
    boolean requestRemoteUpdate(ITemplateKey key);

    /**
     * Requests a remote update for the specified target.
     *
     * @param target The target for which to request an update
     * @see #requestRemoteUpdate(ITemplateKey)
     */
    boolean requestRemoteUpdate(ITemplateKey key, Target target);


    /**
     * Registers an Update Listener - it will only be weakly referenced!
     */
    void registerUpdateListener(IUpdateListener listener);

    void removeUpdateListener(IUpdateListener listener);

    interface IUpdateListener {
        default void onTemplateUpdate(ITemplateProvider provider, ITemplateKey key, Template template) {

        }

        default void onTemplateUpdateSend(ITemplateProvider provider, ITemplateKey key, Template template) {

        }
    }
}
