package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;

/**
 * Weather control symbols.
 */
public final class WeatherSymbols {

    private WeatherSymbols() {}

    public static void register() {
        SymbolRegistry.register(new WeatherNormal());
        SymbolRegistry.register(new WeatherOff());
        SymbolRegistry.register(new WeatherAlways());
        SymbolRegistry.register(new WeatherRain());
        SymbolRegistry.register(new WeatherSnow());
        SymbolRegistry.register(new WeatherStorm());
        SymbolRegistry.register(new WeatherCloudy());
    }

    public static class WeatherNormal extends SymbolBase {
        public WeatherNormal() {
            super(SymbolRegistry.mystcraftId("weather_normal"), SymbolCategory.WEATHER);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Sustain", "Dynamic", "Tradition", "Balance");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setWeatherType("normal");
        }
    }

    public static class WeatherOff extends SymbolBase {
        public WeatherOff() {
            super(SymbolRegistry.mystcraftId("weather_off"), SymbolCategory.WEATHER);
            setCardRank(3);
            setInstabilityCost(0.0f);
            setPoem("Sustain", "Static", "Stimulate", "Energy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setWeatherType("off");
        }
    }

    public static class WeatherAlways extends SymbolBase {
        public WeatherAlways() {
            super(SymbolRegistry.mystcraftId("weather_always"), SymbolCategory.WEATHER);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Sustain", "Static", "Tradition", "Stimulate");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setWeatherType("always");
            director.addInstability(getInstabilityCost());
        }
    }

    public static class WeatherRain extends SymbolBase {
        public WeatherRain() {
            super(SymbolRegistry.mystcraftId("weather_rain"), SymbolCategory.WEATHER);
            setCardRank(3);
            setInstabilityCost(3.0f);
            setPoem("Sustain", "Static", "Rebirth", "Growth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setWeatherType("rain");
            director.addInstability(getInstabilityCost());
        }
    }

    public static class WeatherSnow extends SymbolBase {
        public WeatherSnow() {
            super(SymbolRegistry.mystcraftId("weather_snow"), SymbolCategory.WEATHER);
            setCardRank(3);
            setInstabilityCost(3.0f);
            setPoem("Sustain", "Static", "Inhibit", "Energy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setWeatherType("snow");
            director.addInstability(getInstabilityCost());
        }
    }

    public static class WeatherStorm extends SymbolBase {
        public WeatherStorm() {
            super(SymbolRegistry.mystcraftId("weather_storm"), SymbolCategory.WEATHER);
            setCardRank(3);
            setInstabilityCost(8.0f);
            setPoem("Sustain", "Static", "Nature", "Power");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setWeatherType("storm");
            director.setLightningEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class WeatherCloudy extends SymbolBase {
        public WeatherCloudy() {
            super(SymbolRegistry.mystcraftId("weather_cloudy"), SymbolCategory.WEATHER);
            setCardRank(3);
            setInstabilityCost(0.0f);
            setPoem("Sustain", "Static", "Believe", "Motion");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setWeatherType("cloudy");
        }
    }
}
