package CardAugments.patches;

import CardAugments.CardAugmentsMod;
import CardAugments.cardmods.AbstractAugment;
import CardAugments.cardmods.common.MassiveMod;
import CardAugments.cardmods.common.TinyMod;
import CardAugments.cardmods.uncommon.BlurryMod;
import SpireLocations.patches.nodemodifierhooks.ModifyRewardsPatch;
import basemod.abstracts.AbstractCardModifier;
import basemod.helpers.CardModifierManager;
import blurryblur.CardPatches;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.green.Blur;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.screens.CombatRewardScreen;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import rainbowMod.patches.RainbowCardsInChestsPatch;
import spireTogether.network.objects.NetworkClassObject;
import spireTogether.util.DevConfig;
import spireTogether.util.Reflection;
import spireTogether.util.SerializablePair;
import spireTogether.util.SpireLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static CardAugments.CardAugmentsMod.rollCardAugment;

public class CompatibilityPatches {
    @SpirePatch2(clz = ModifyRewardsPatch.class, method = "addModifiersRewards", requiredModId = "spirelocations", optional = true)
    public static class HitSpireLocationsRewards {
        @SpirePostfixPatch
        public static void plz(Object[] __args) {
            if (__args[0] instanceof CombatRewardScreen) {
                OnCardGeneratedPatches.ModifyRewardScreenStuff.patch((CombatRewardScreen) __args[0]);
            }
        }
    }

    @SpirePatch2(clz = visiblecardrewards.rewards.SingleCardReward.class, method = "init", requiredModId = "visiblecardrewards", optional = true)
    public static class UseModifiedNames {
        @SpirePostfixPatch
        public static void plz(visiblecardrewards.rewards.SingleCardReward __instance) {
            if (__instance.type == visiblecardrewards.patches.NewRewardtypePatch.VCR_SINGLECARDREWARD) {
                __instance.text = CardModifierManager.onRenderTitle(__instance.card, __instance.card.name);
            }
        }
    }

    @SpirePatch2(clz = oceanmod.rewards.SingleCardReward.class, method = "init", requiredModId = "oceanmod", optional = true)
    public static class UseModifiedNames2 {
        @SpirePostfixPatch
        public static void plz(oceanmod.rewards.SingleCardReward __instance) {
            if (__instance.type == oceanmod.patches.visiblecardrewards.NewRewardtypePatch.VCR_SINGLECARDREWARD) {
                __instance.text = CardModifierManager.onRenderTitle(__instance.card, __instance.card.name);
            }
        }
    }

    @SpirePatch2(clz = AbstractCard.class, method = "render", paramtypez = {SpriteBatch.class}, requiredModId = "bigcards", optional = true)
    public static class SizeChanges {
        @SpirePrefixPatch
        public static void changeSize(AbstractCard __instance) {
            if (CardModifierManager.hasModifier(__instance, MassiveMod.ID)) {
                __instance.drawScale *= 2/1.5f;
            }
            if (CardModifierManager.hasModifier(__instance, TinyMod.ID)) {
                __instance.drawScale *= 1.5/2f;
            }
        }
        @SpirePostfixPatch
        public static void resetSize(AbstractCard __instance) {
            if (CardModifierManager.hasModifier(__instance, MassiveMod.ID)) {
                __instance.drawScale /= 2/1.5f;
            }
            if (CardModifierManager.hasModifier(__instance, TinyMod.ID)) {
                __instance.drawScale /= 1.5/2f;
            }
        }
    }

    @SpirePatch2(clz = AbstractCard.class, method = "renderHoverShadow", requiredModId = "bigcards", optional = true)
    public static class FixShadow {
        @SpireInstrumentPatch
        public static ExprEditor plz() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getClassName().equals(AbstractCard.class.getName()) && m.getMethodName().equals("renderHelper")) {
                        m.replace("$6 = "+CompatibilityPatches.class.getName()+".getScale($0, $6); $_ = $proceed($$);");
                    }
                }
            };
        }
    }

    public static float getScale(AbstractCard card, float scale) {
        if (CardModifierManager.hasModifier(card, MassiveMod.ID)) {
            scale *= 2/1.5f;
        }
        if (CardModifierManager.hasModifier(card, TinyMod.ID)) {
            scale *= 1.5/2f;
        }
        return scale;
    }

    @SpirePatch2(clz = CardPatches.NormalRender.class, method = "InsertBefore", requiredModId = "blurryblur", optional = true)
    public static class BlurOn {
        @SpirePostfixPatch
        public static void plz(Object[] __args) {
            if (__args[1] instanceof SpriteBatch && __args[0] instanceof AbstractCard) {
                if (CardModifierManager.hasModifier((AbstractCard) __args[0], BlurryMod.ID)) {
                    if (__args[0] instanceof Blur) {
                        CardPatches.INSTANCE.begin((Batch) __args[1], 4.0f);
                    } else {
                        CardPatches.INSTANCE.begin((Batch) __args[1], 2.0f);
                    }
                }
            }
        }
    }

    @SpirePatch2(clz = CardPatches.NormalRender.class, method = "InsertAfter", requiredModId = "blurryblur", optional = true)
    public static class BlurOff {
        @SpirePostfixPatch
        public static void plz(Object[] __args) {
            if (__args[1] instanceof SpriteBatch && __args[0] instanceof AbstractCard) {
                if (CardModifierManager.hasModifier((AbstractCard) __args[0], BlurryMod.ID)) {
                    CardPatches.INSTANCE.end((Batch) __args[1]);
                }
            }
        }
    }

    @SpirePatch2(clz = CardPatches.SCVRender.class, method = "InsertBefore", requiredModId = "blurryblur", optional = true)
    public static class BlurOnSCV {
        @SpirePostfixPatch
        public static void plz(Object[] __args) {
            if (__args[0] instanceof SpriteBatch && __args[1] instanceof AbstractCard) {
                if (CardModifierManager.hasModifier((AbstractCard) __args[1], BlurryMod.ID)) {
                    if (__args[1] instanceof Blur) {
                        CardPatches.INSTANCE.begin((Batch) __args[0], 8.0f);
                    } else {
                        CardPatches.INSTANCE.begin((Batch) __args[0], 4.0f);
                    }
                }
            }
        }
    }

    @SpirePatch2(clz = CardPatches.SCVRender.class, method = "InsertAfter", requiredModId = "blurryblur", optional = true)
    public static class BlurOffSCV {
        @SpirePostfixPatch
        public static void plz(Object[] __args) {
            if (__args[0] instanceof SpriteBatch && __args[1] instanceof AbstractCard) {
                if (CardModifierManager.hasModifier((AbstractCard) __args[1], BlurryMod.ID)) {
                    CardPatches.INSTANCE.end((Batch) __args[0]);
                }
            }
        }
    }

    @SpirePatch2(clz = RainbowCardsInChestsPatch.class, method = "makeRainbowReward", requiredModId = "RainbowMod", optional = true)
    public static class ModsOnRainbowCards {
        @SpirePostfixPatch
        public static void modTime(RewardItem __result) {
            if (__result.cards != null) {
                for (AbstractCard c : __result.cards) {
                    rollCardAugment(c);
                }
            }
        }
    }

    @SpirePatch(clz = AbstractCard.class, method = "<class>")
    public static class CardModifiersMirrorField {
        public static SpireField<String> cardModifiersSerialized = new SpireField<>(() -> "");
    }

    @SpirePatch2(clz = NetworkClassObject.class, method = "CopyValues", requiredModId = "spireTogether", optional = true)
    public static class NetworkSerializeCardAugments {

        @SpirePrefixPatch
        public static <T extends NetworkClassObject> void CopyValuesPatch(Object original) {
            if (original instanceof AbstractCard)
            {
                // serialize modifiers to string string
                AbstractCard card = (AbstractCard)original;
                ArrayList<AbstractCardModifier> modifiers = CardModifierManager.modifiers(card);
                StringJoiner sj = new StringJoiner(",");
                for (AbstractCardModifier modifier : modifiers)
                {
                    if (modifier instanceof AbstractAugment)
                    {
                        AbstractAugment augment = (AbstractAugment) modifier;
                        sj.add(augment.getClass().getCanonicalName() + ":" + augment.toNetworkData());
                    }
                }
                CardAugmentsMod.logger.info("Modifiers serialized to: {}", sj.toString());
                CompatibilityPatches.CardModifiersMirrorField.cardModifiersSerialized.set(card, sj.toString());
            }
        }

        public static void loadModifiers(AbstractCard card, String modifiersStr)
        {
            if (modifiersStr.isEmpty())
            {
                return;
            }
            String[] modifiers = modifiersStr.split(",");
            for (String modifier : modifiers)
            {
                String[] words = modifier.split(":", 2);
                if (words.length != 2)
                {
                    CardAugmentsMod.logger.error("loadModifiers failed: invalid format: {}", modifier);
                    continue;
                }
                String modifierName = words[0];
                String modifierData = words[1];
                try {
                    Class<?> clazz = Class.forName(modifierName);
                    if (AbstractAugment.class.isAssignableFrom(clazz)) {
                        @SuppressWarnings("unchecked")
                        Class<? extends AbstractAugment> modifierClass = (Class<? extends AbstractAugment>) clazz;
                        Constructor<? extends AbstractAugment> constructor = modifierClass.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        AbstractAugment augment = constructor.newInstance();
                        augment.fromNetworkData(modifierData);
                        CardModifierManager.addModifier(card, augment);
                        CardAugmentsMod.logger.info("Successfully deserialized: {}:{}", modifierName, modifierData);
                    }
                    else
                    {
                        CardAugmentsMod.logger.error("loadModifiers failed: not a subclass of AbstractAugment: {}", modifierName);
                    }
                } catch (ClassNotFoundException e) {
                    CardAugmentsMod.logger.error("loadModifiers failed: Class not found: {}", modifierName);
                } catch (NoSuchMethodException e) {
                    CardAugmentsMod.logger.error("loadModifiers failed: No no-arg constructor found: {}", modifierName);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to instantiate: " + modifierName, e);
                }
            }
        }
    }

    public static class NetworkDeserializeCardAugmentsUtils
    {
        private static HashMap<String, Field> fieldCache = new HashMap<String, Field>();

        private static String demangle(String name)
        {
            return name.replaceAll("_\\d+$", "");
        }

        private static Field findField(Class<?> oClass, String fieldName, ArrayList<Map<String, Field>> fields)
        {
            String key = oClass.getCanonicalName() + ":" + fieldName;
            if (fieldCache.containsKey(key))
            {
                return fieldCache.get(key);
            }
            // exact match
            for(Map<String, Field> objectFields : fields) {
                if (objectFields != null) {
                    Field f = objectFields.get(fieldName);
                    if (f != null) {
                        fieldCache.put(key, f);
                        return f;
                    }
                }
            }
            // mangled match
            String demangled = demangle(fieldName);
            for(Map<String, Field> objectFields : fields) {
                if (objectFields != null) {
                    for (String s : objectFields.keySet())
                    {
                        if (demangle(s).equals(demangled))
                        {
                            Field f = objectFields.get(s);
                            if (f != null) {
                                fieldCache.put(key, f);
                                return f;
                            }
                        }
                    }
                }
            }
            return null;
        }

        public static Field getFieldByName(Class<?> oClass, String fieldName) throws NoSuchFieldException {
            ArrayList<Map<String, Field>> fields = null;
            try
            {
                Method method = Reflection.class.getDeclaredMethod("getAllFields", Class.class);
                method.setAccessible(true);
                Object o = method.invoke(null, oClass);
                fields = (ArrayList<Map<String, Field>>)o;
            }
            catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
            {
                SpireLogger.Log("Method getAllFields failed: {}", e.toString());
                throw new NoSuchFieldException();
            }
            Field f = NetworkDeserializeCardAugmentsUtils.findField(oClass, fieldName, fields);
            if (f != null)
            {
                return f;
            }
            throw new NoSuchFieldException();
        }
    }

    @SpirePatch2(clz = Reflection.class, method = "setFieldValue", requiredModId = "spireTogether", optional = true)
    public static class NetworkDeserializeCardAugments
    {
        @SpirePostfixPatch
        public static void SetFieldValuePatch(String varName, Object source, Object value) {
            if (value != null) {
                try {
                    Field field = NetworkDeserializeCardAugmentsUtils.getFieldByName(source.getClass(), varName);
                    field.setAccessible(true);
                    field.set(source, value);
                } catch (Exception e) {
                    if (DevConfig.logMode) {
                        SpireLogger.LogError("Error setting field " + varName + " with value " + (value == null ? "null" : value) + "due to " + e, SpireLogger.ErrorType.FATAL);
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
