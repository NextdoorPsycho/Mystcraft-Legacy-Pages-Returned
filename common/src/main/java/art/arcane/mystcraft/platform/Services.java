package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.*;

import java.util.ServiceLoader;

// ServiceLoader utility for loading platform implementations
public final class Services {

  public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
  public static final IRegistrationHelper REGISTRATION = load(IRegistrationHelper.class);
  public static final INetworkHelper NETWORK = load(INetworkHelper.class);
  public static final IEventHelper EVENTS = load(IEventHelper.class);
  public static final IClientHelper CLIENT = load(IClientHelper.class);

  private Services() {
  }

  public static <T> T load(Class<T> clazz) {
    T loadedService = ServiceLoader.load(clazz)
        .findFirst()
        .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    return loadedService;
  }
}
