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
        SymbolRegistry.register(new WeatherFast());
        SymbolRegistry.register(new WeatherSlow());
        SymbolRegistry.register(new WeatherThunder());
        SymbolRegistry.register(new WeatherBlizzard());
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

    public static class WeatherFast extends SymbolBase {
        public WeatherFast() {
            super(SymbolRegistry.mystcraftId("weather_fast"), SymbolCategory.WEATHER);
            setCardRank(3);
            setInstabilityCost(3.0f);
            setPoem("Sustain", "Dynamic", "Spur", "Change");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setWeatherType("fast");
            director.addInstability(getInstabilityCost());
        }
    }

    public static class WeatherSlow extends SymbolBase {
        public WeatherSlow() {
            super(SymbolRegistry.mystcraftId("weather_slow"), SymbolCategory.WEATHER);
            setCardRank(3);
            setInstabilityCost(2.0f);
            setPoem("Sustain", "Dynamic", "Inhibit", "Motion");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setWeatherType("slow");
            director.addInstability(getInstabilityCost());
        }
    }

    public static class WeatherThunder extends SymbolBase {
        public WeatherThunder() {
            super(SymbolRegistry.mystcraftId("weather_thunder"), SymbolCategory.WEATHER);
            setCardRank(3);
            setInstabilityCost(10.0f);
            setPoem("Sustain", "Static", "Power", "Lightning");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setWeatherType("thunder");
            director.setLightningEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class WeatherBlizzard extends SymbolBase {
        public WeatherBlizzard() {
            super(SymbolRegistry.mystcraftId("weather_blizzard"), SymbolCategory.WEATHER);
            setCardRank(3);
            setInstabilityCost(12.0f);
            setPoem("Sustain", "Static", "Ice", "Fury");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setWeatherType("blizzard");
            director.addInstability(getInstabilityCost());
        }
    }
}
