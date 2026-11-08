package dev.geng.application.media;

import java.io.InputStream;
import java.io.IOException;
import java.util.UUID;

/** Authorized use cases only. Implementations never return permanent public URLs. */
public interface PrivateMediaReader {
    /** Opens an immutable object version after use-case authorization; caller closes it. */
    InputStream open(UUID assetId, String objectVersion) throws IOException;
}
