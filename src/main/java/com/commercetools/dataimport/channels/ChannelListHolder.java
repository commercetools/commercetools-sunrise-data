package com.commercetools.dataimport.channels;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.models.Base;

import java.util.Collections;
import java.util.List;

public class ChannelListHolder extends Base {

    private final List<Channel> channels;

    public ChannelListHolder(final List<Channel> channels) {
        this.channels = Collections.unmodifiableList(channels);
    }

    public List<Channel> getChannels() {
        return channels;
    }
}
