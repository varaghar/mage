package mage.cards;

import mage.MageObject;
import mage.MageObjectImpl;
import mage.Mana;
import mage.ObjectColor;
import mage.abilities.*;
import mage.abilities.keyword.FlashbackAbility;
import mage.abilities.mana.ActivatedManaAbilityImpl;
import mage.cards.repository.PluginClassloaderRegistery;
import mage.constants.*;
import mage.counters.Counter;
import mage.counters.Counters;
import mage.filter.FilterMana;
import mage.game.*;
import mage.game.command.CommandObject;
import mage.game.events.*;
import mage.game.permanent.Permanent;
import mage.game.permanent.PermanentCard;
import mage.game.stack.Spell;
import mage.game.stack.StackObject;
import mage.util.CardUtil;
import mage.util.GameLog;
import mage.watchers.Watcher;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class CardImpl extends MageObjectImpl implements Card {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(CardImpl.class);

    private static final String regexBlack = ".*\\x7b.{0,2}B.{0,2}\\x7d.*";
    private static final String regexBlue = ".*\\x7b.{0,2}U.{0,2}\\x7d.*";
    private static final String regexRed = ".*\\x7b.{0,2}R.{0,2}\\x7d.*";
    private static final String regexGreen = ".*\\x7b.{0,2}G.{0,2}\\x7d.*";
    private static final String regexWhite = ".*\\x7b.{0,2}W.{0,2}\\x7d.*";

    protected UUID ownerId;
    protected String cardNumber;
    protected String expansionSetCode;
    protected String tokenSetCode;
    protected String tokenDescriptor;
    protected Rarity rarity;
    protected boolean transformable;
    protected Class<?> secondSideCardClazz;
    protected Card secondSideCard;
    protected boolean nightCard;
    protected SpellAbility spellAbility;
    protected boolean flipCard;
    protected String flipCardName;
    protected boolean usesVariousArt = false;
    protected boolean morphCard;
    protected List<UUID> attachments = new ArrayList<>();

    public CardImpl(UUID ownerId, CardSetInfo setInfo, CardType[] cardTypes, String costs) {
        this(ownerId, setInfo, cardTypes, costs, SpellAbilityType.BASE);
    }

    public CardImpl(UUID ownerId, CardSetInfo setInfo, CardType[] cardTypes, String costs, SpellAbilityType spellAbilityType) {
        this(ownerId, setInfo.getName());
        this.rarity = setInfo.getRarity();
        this.cardNumber = setInfo.getCardNumber();
        this.expansionSetCode = setInfo.getExpansionSetCode();
        this.cardType.addAll(Arrays.asList(cardTypes));
        this.manaCost.load(costs);
        setDefaultColor();
        if (this.isLand()) {
            Ability ability = new PlayLandAbility(name);
            ability.setSourceId(this.getId());
            abilities.add(ability);
        } else {
            SpellAbility ability = new SpellAbility(manaCost, name, Zone.HAND, spellAbilityType);
            if (!this.isInstant()) {
                ability.setTiming(TimingRule.SORCERY);
            }
            ability.setSourceId(this.getId());
            abilities.add(ability);
        }

        CardGraphicInfo graphicInfo = setInfo.getGraphicInfo();
        if (graphicInfo != null) {
            this.usesVariousArt = graphicInfo.getUsesVariousArt();
            if (graphicInfo.getFrameColor() != null) {
                this.frameColor = graphicInfo.getFrameColor().copy();
            }
            if (graphicInfo.getFrameStyle() != null) {
                this.frameStyle = graphicInfo.getFrameStyle();
            }
        }

        this.morphCard = false;
    }

    private void setDefaultColor() {
        this.color.setWhite(this.manaCost.containsColor(ColoredManaSymbol.W));
        this.color.setBlue(this.manaCost.containsColor(ColoredManaSymbol.U));
        this.color.setBlack(this.manaCost.containsColor(ColoredManaSymbol.B));
        this.color.setRed(this.manaCost.containsColor(ColoredManaSymbol.R));
        this.color.setGreen(this.manaCost.containsColor(ColoredManaSymbol.G));
    }

    protected CardImpl(UUID ownerId, String name) {
        this.ownerId = ownerId;
        this.name = name;
    }

    protected CardImpl(UUID id, UUID ownerId, String name) {
        super(id);
        this.ownerId = ownerId;
        this.name = name;
    }

    public CardImpl(final CardImpl card) {
        super(card);
        ownerId = card.ownerId;
        cardNumber = card.cardNumber;
        expansionSetCode = card.expansionSetCode;
        tokenSetCode = card.tokenSetCode;
        tokenDescriptor = card.tokenDescriptor;
        rarity = card.rarity;

        transformable = card.transformable;
        secondSideCardClazz = card.secondSideCardClazz;
        secondSideCard = null; // will be set on first getSecondCardFace call if card has one
        nightCard = card.nightCard;

        spellAbility = null; // will be set on first getSpellAbility call if card has one
        flipCard = card.flipCard;
        flipCardName = card.flipCardName;
        usesVariousArt = card.usesVariousArt;
        morphCard = card.morphCard;

        this.attachments.addAll(card.attachments);
    }

    @Override
    public void assignNewId() {
        this.objectId = UUID.randomUUID();
        this.abilities.newOriginalId();
        this.abilities.setSourceId(objectId);
        if (this.spellAbility != null) {
            this.spellAbility.setSourceId(objectId);
        }
    }

    public static Card createCard(String name, CardSetInfo setInfo) {
        try {
            return createCard(Class.forName(name), setInfo);
        } catch (ClassNotFoundException ex) {
            try {
                return createCard(PluginClassloaderRegistery.forName(name), setInfo);
            } catch (ClassNotFoundException ex2) {
                // ignored
            }
            logger.fatal("Error loading card: " + name, ex);
            return null;
        }
    }

    public static Card createCard(Class<?> clazz, CardSetInfo setInfo) {
        return createCard(clazz, setInfo, null);
    }

    public static Card createCard(Class<?> clazz, CardSetInfo setInfo, List<String> errorList) {
        String setCode = null;
        try {
            Card card;
            if (setInfo == null) {
                Constructor<?> con = clazz.getConstructor(UUID.class);
                card = (Card) con.newInstance(new Object[]{null});
            } else {
                setCode = setInfo.getExpansionSetCode();
                Constructor<?> con = clazz.getConstructor(UUID.class, CardSetInfo.class);
                card = (Card) con.newInstance(null, setInfo);
            }
            return card;
        } catch (Exception e) {
            String err = "Error loading card: " + clazz.getCanonicalName() + " (" + setCode + ")";
            if (errorList != null) {
                errorList.add(err);
            }

            if (e instanceof InvocationTargetException) {
                logger.fatal(err, ((InvocationTargetException) e).getTargetException());
            } else {
                logger.fatal(err, e);
            }

            return null;
        }
    }

    @Override
    public UUID getOwnerId() {
        return ownerId;
    }

    @Override
    public String getCardNumber() {
        return cardNumber;
    }

    @Override
    public Rarity getRarity() {
        return rarity;
    }

    @Override
    public void addInfo(String key, String value, Game game) {
        game.getState().getCardState(objectId).addInfo(key, value);
    }

    @Override
    public List<String> getRules() {
        Abilities<Ability> sourceAbilities = this.getAbilities();
        return CardUtil.getCardRulesWithAdditionalInfo(this.getId(), this.getName(), sourceAbilities, sourceAbilities);
    }

    @Override
    public List<String> getRules(Game game) {
        Abilities<Ability> sourceAbilities = this.getAbilities(game);
        return CardUtil.getCardRulesWithAdditionalInfo(game, this.getId(), this.getName(), sourceAbilities, sourceAbilities);
    }

    /**
     * Gets all base abilities - does not include additional abilities added by
     * other cards or effects
     *
     * @return A list of {@link Ability} - this collection is modifiable
     */
    @Override
    public Abilities<Ability> getAbilities() {
        return super.getAbilities();
    }

    /**
     * Gets all current abilities - includes additional abilities added by other
     * cards or effects. Warning, you can't modify that list.
     *
     * @param game
     * @return A list of {@link Ability} - this collection is not modifiable
     */
    @Override
    public Abilities<Ability> getAbilities(Game game) {
        if (game == null) {
            return abilities; // deck editor with empty game
        }

        CardState cardState = game.getState().getCardState(this.getId());
        if (cardState == null) {
            return abilities;
        }

        // collects all abilities
        Abilities<Ability> all = new AbilitiesImpl<>();

        // basic
        if (!cardState.hasLostAllAbilities()) {
            all.addAll(abilities);
        }

        // dynamic
        all.addAll(cardState.getAbilities());

        // workaround to add dynamic flashback ability from main card to all parts (example: Snapcaster Mage gives flashback to split card)
        if (!this.getId().equals(this.getMainCard().getId())) {
            CardState mainCardState = game.getState().getCardState(this.getMainCard().getId());
            if (this.getSpellAbility() != null // lands can't be casted (haven't spell ability), so ignore it
                    && mainCardState != null
                    && !mainCardState.hasLostAllAbilities()
                    && mainCardState.getAbilities().containsClass(FlashbackAbility.class)) {
                FlashbackAbility flash = new FlashbackAbility(this.getManaCost(), this.isInstant() ? TimingRule.INSTANT : TimingRule.SORCERY);
                flash.setSourceId(this.getId());
                flash.setControllerId(this.getOwnerId());
                flash.setSpellAbilityType(this.getSpellAbility().getSpellAbilityType());
                flash.setAbilityName(this.getName());
                all.add(flash);
            }
        }

        return all;
    }

    @Override
    public void looseAllAbilities(Game game) {
        CardState cardState = game.getState().getCardState(this.getId());
        cardState.setLostAllAbilities(true);
        cardState.getAbilities().clear();
    }

    @Override
    public boolean hasAbility(Ability ability, Game game) {
        // getAbilities(game) searches all abilities from base and dynamic lists (other)
        return this.getAbilities(game).contains(ability);
    }

    /**
     * Public in order to support adding abilities to SplitCardHalf's
     *
     * @param ability
     */
    @Override
    public void addAbility(Ability ability) {
        ability.setSourceId(this.getId());
        abilities.add(ability);
        for (Ability subAbility : ability.getSubAbilities()) {
            abilities.add(subAbility);
        }

        // dynamic check: you can't add ability to the PermanentCard, use permanent.addAbility(a, source, game) instead
        // reason: triggered abilities are not processing here
        if (this instanceof PermanentCard) {
            throw new IllegalArgumentException("Wrong code usage. Don't use that method for permanents, use permanent.addAbility(a, source, game) instead.");
        }

        // verify check: draw effect can't be rollback after mana usage (example: Chromatic Sphere)
        // (player can cheat with cancel button to see next card)
        // verify test will catch that errors
        if (ability instanceof ActivatedManaAbilityImpl) {
            ActivatedManaAbilityImpl manaAbility = (ActivatedManaAbilityImpl) ability;
            String rule = manaAbility.getRule().toLowerCase(Locale.ENGLISH);
            if (manaAbility.getEffects().stream().anyMatch(e -> e.getOutcome().equals(Outcome.DrawCard))
                    || rule.contains("reveal ")
                    || rule.contains("draw ")) {
                if (manaAbility.isUndoPossible()) {
                    throw new IllegalArgumentException("Ability contains draw/reveal effect, but isUndoPossible is true. Ability: "
                            + ability.getClass().getSimpleName() + "; " + ability.getRule());
                }
            }
        }
    }

    protected void addAbilities(List<Ability> abilities) {
        for (Ability ability : abilities) {
            addAbility(ability);
        }
    }

    protected void addAbility(Ability ability, Watcher watcher) {
        addAbility(ability);
        ability.addWatcher(watcher);
    }

    public void replaceSpellAbility(SpellAbility newAbility) {
        SpellAbility oldAbility = this.getSpellAbility();
        while (oldAbility != null) {
            abilities.remove(oldAbility);
            spellAbility = null;
            oldAbility = this.getSpellAbility();
        }

        if (newAbility != null) {
            addAbility(newAbility);
        }
    }

    @Override
    public SpellAbility getSpellAbility() {
        if (spellAbility == null) {
            for (Ability ability : abilities.getActivatedAbilities(Zone.HAND)) {
                if (ability instanceof SpellAbility
                        && ((SpellAbility) ability).getSpellAbilityType() != SpellAbilityType.BASE_ALTERNATE) {
                    return spellAbility = (SpellAbility) ability;
                }
            }
        }
        return spellAbility;
    }

    /**
     * Dynamic cost modification for card (process only own abilities). Example:
     * if it need stack related info (like real targets) then must check two
     * states (game.inCheckPlayableState):
     * <p>
     * 1. In playable state it must check all possible use cases (e.g. allow to
     * reduce on any available target and modes)
     * <p>
     * 2. In real cast state it must check current use case (e.g. real selected
     * targets and modes)
     *
     * @param ability
     * @param game
     */
    @Override
    public void adjustCosts(Ability ability, Game game) {
        ability.adjustCosts(game);
    }

    @Override
    public void adjustTargets(Ability ability, Game game) {
        ability.adjustTargets(game);
    }

    @Override
    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
        abilities.setControllerId(ownerId);
    }

    @Override
    public String getExpansionSetCode() {
        return expansionSetCode;
    }

    @Override
    public String getTokenSetCode() {
        return tokenSetCode;
    }

    @Override
    public String getTokenDescriptor() {
        return tokenDescriptor;
    }

    @Override
    public List<Mana> getMana() {
        List<Mana> mana = new ArrayList<>();
        for (ActivatedManaAbilityImpl ability : this.abilities.getActivatedManaAbilities(Zone.BATTLEFIELD)) {
            mana.addAll(ability.getNetMana(null));
        }
        return mana;
    }

    @Override
    public boolean moveToZone(Zone toZone, Ability source, Game game, boolean flag) {
        return this.moveToZone(toZone, source, game, flag, null);
    }

    @Override
    public boolean moveToZone(Zone toZone, Ability source, Game game, boolean flag, List<UUID> appliedEffects) {
        Zone fromZone = game.getState().getZone(objectId);
        ZoneChangeEvent event = new ZoneChangeEvent(this.objectId, source, ownerId, fromZone, toZone, appliedEffects);
        ZoneChangeInfo zoneChangeInfo;
        if (null != toZone) {
            switch (toZone) {
                case LIBRARY:
                    zoneChangeInfo = new ZoneChangeInfo.Library(event, flag /* put on top */);
                    break;
                case BATTLEFIELD:
                    zoneChangeInfo = new ZoneChangeInfo.Battlefield(event, flag /* comes into play tapped */, source);
                    break;
                default:
                    zoneChangeInfo = new ZoneChangeInfo(event);
                    break;
            }
            return ZonesHandler.moveCard(zoneChangeInfo, game, source);
        }
        return false;
    }

    @Override
    public boolean cast(Game game, Zone fromZone, SpellAbility ability, UUID controllerId) {
        Card mainCard = getMainCard();
        ZoneChangeEvent event = new ZoneChangeEvent(mainCard.getId(), ability, controllerId, fromZone, Zone.STACK);
        Spell spell = new Spell(this, ability.getSpellAbilityToResolve(game), controllerId, event.getFromZone(), game);
        ZoneChangeInfo.Stack info = new ZoneChangeInfo.Stack(event, spell);
        return ZonesHandler.cast(info, game, ability);
    }

    @Override
    public boolean moveToExile(UUID exileId, String name, Ability source, Game game) {
        return moveToExile(exileId, name, source, game, null);
    }

    @Override
    public boolean moveToExile(UUID exileId, String name, Ability source, Game game, List<UUID> appliedEffects) {
        Zone fromZone = game.getState().getZone(objectId);
        ZoneChangeEvent event = new ZoneChangeEvent(this.objectId, source, ownerId, fromZone, Zone.EXILED, appliedEffects);
        ZoneChangeInfo.Exile info = new ZoneChangeInfo.Exile(event, exileId, name);
        return ZonesHandler.moveCard(info, game, source);
    }

    @Override
    public boolean putOntoBattlefield(Game game, Zone fromZone, Ability source, UUID controllerId) {
        return this.putOntoBattlefield(game, fromZone, source, controllerId, false, false, null);
    }

    @Override
    public boolean putOntoBattlefield(Game game, Zone fromZone, Ability source, UUID controllerId, boolean tapped) {
        return this.putOntoBattlefield(game, fromZone, source, controllerId, tapped, false, null);
    }

    @Override
    public boolean putOntoBattlefield(Game game, Zone fromZone, Ability source, UUID controllerId, boolean tapped, boolean faceDown) {
        return this.putOntoBattlefield(game, fromZone, source, controllerId, tapped, faceDown, null);
    }

    @Override
    public boolean putOntoBattlefield(Game game, Zone fromZone, Ability source, UUID controllerId, boolean tapped, boolean faceDown, List<UUID> appliedEffects) {
        ZoneChangeEvent event = new ZoneChangeEvent(this.objectId, source, controllerId, fromZone, Zone.BATTLEFIELD, appliedEffects);
        ZoneChangeInfo.Battlefield info = new ZoneChangeInfo.Battlefield(event, faceDown, tapped, source);
        return ZonesHandler.moveCard(info, game, source);
    }

    @Override
    public boolean removeFromZone(Game game, Zone fromZone, Ability source) {
        boolean removed = false;
        MageObject lkiObject = null;
        switch (fromZone) {
            case GRAVEYARD:
                removed = game.getPlayer(ownerId).removeFromGraveyard(this, game);
                break;
            case HAND:
                removed = game.getPlayer(ownerId).removeFromHand(this, game);
                break;
            case LIBRARY:
                removed = game.getPlayer(ownerId).removeFromLibrary(this, game);
                break;
            case EXILED:
                if (game.getExile().getCard(getId(), game) != null) {
                    removed = game.getExile().removeCard(this, game);
                }
                break;
            case STACK:
                StackObject stackObject;
                if (getSpellAbility() != null) {
                    stackObject = game.getStack().getSpell(getSpellAbility().getId(), false);
                } else {
                    stackObject = game.getStack().getSpell(this.getId(), false);
                }

                // handle half of Split Cards on stack
                if (stackObject == null && (this instanceof SplitCard)) {
                    stackObject = game.getStack().getSpell(((SplitCard) this).getLeftHalfCard().getId(), false);
                    if (stackObject == null) {
                        stackObject = game.getStack().getSpell(((SplitCard) this).getRightHalfCard().getId(), false);
                    }
                }

                // handle half of Modal Double Faces Cards on stack
                if (stackObject == null && (this instanceof ModalDoubleFacesCard)) {
                    stackObject = game.getStack().getSpell(((ModalDoubleFacesCard) this).getLeftHalfCard().getId(), false);
                    if (stackObject == null) {
                        stackObject = game.getStack().getSpell(((ModalDoubleFacesCard) this).getRightHalfCard().getId(), false);
                    }
                }

                if (stackObject == null && (this instanceof AdventureCard)) {
                    stackObject = game.getStack().getSpell(((AdventureCard) this).getSpellCard().getId(), false);
                }

                if (stackObject == null) {
                    stackObject = game.getStack().getSpell(getId(), false);
                }
                if (stackObject != null) {
                    removed = game.getStack().remove(stackObject, game);
                    lkiObject = stackObject;
                }
                break;
            case COMMAND:
                for (CommandObject commandObject : game.getState().getCommand()) {
                    if (commandObject.getId().equals(objectId)) {
                        lkiObject = commandObject;
                    }
                }
                if (lkiObject != null) {
                    removed = game.getState().getCommand().remove(lkiObject);
                }
                break;
            case OUTSIDE:
                if (isCopy()) { // copied cards have no need to be removed from a previous zone
                    removed = true;
                } else if (game.getPlayer(ownerId).getSideboard().contains(this.getId())) {
                    game.getPlayer(ownerId).getSideboard().remove(this.getId());
                    removed = true;
                } else if (game.getPhase() == null) {
                    // E.g. Commander of commander game
                    removed = true;
                } else {
                    // Unstable - Summon the Pack
                    removed = true;
                }
                break;
            case BATTLEFIELD: // for sacrificing permanents or putting to library
                removed = true;
                break;
            default:
                MageObject sourceObject = game.getObject(source.getSourceId());
                logger.fatal("Invalid from zone [" + fromZone + "] for card [" + this.getIdName()
                        + "] source [" + (sourceObject != null ? sourceObject.getName() : "null") + ']');
                break;
        }
        if (removed) {
            if (fromZone != Zone.OUTSIDE) {
                game.rememberLKI(lkiObject != null ? lkiObject.getId() : objectId, fromZone, lkiObject != null ? lkiObject : this);
            }
        } else {
            logger.warn("Couldn't find card in fromZone, card=" + getIdName() + ", fromZone=" + fromZone);
            // possible reason: you to remove card from wrong zone or card already removed,
            // e.g. you added copy card to wrong graveyard (see owner) or removed card from graveyard before moveToZone call
        }
        return removed;
    }

    @Override
    public void checkForCountersToAdd(Permanent permanent, Ability source, Game game) {
        Counters countersToAdd = game.getEnterWithCounters(permanent.getId());
        if (countersToAdd != null) {
            for (Counter counter : countersToAdd.values()) {
                permanent.addCounters(counter, source, game);
            }
            game.setEnterWithCounters(permanent.getId(), null);
        }
    }

    @Override
    public void setFaceDown(boolean value, Game game) {
        game.getState().getCardState(objectId).setFaceDown(value);
    }

    @Override
    public boolean isFaceDown(Game game) {
        return game.getState().getCardState(objectId).isFaceDown();
    }

    @Override
    public boolean turnFaceUp(Ability source, Game game, UUID playerId) {
        GameEvent event = GameEvent.getEvent(GameEvent.EventType.TURNFACEUP, getId(), source, playerId);
        if (!game.replaceEvent(event)) {
            setFaceDown(false, game);
            for (Ability ability : abilities) { // abilities that were set to not visible face down must be set to visible again
                if (ability.getWorksFaceDown() && !ability.getRuleVisible()) {
                    ability.setRuleVisible(true);
                }
            }
            game.fireEvent(GameEvent.getEvent(GameEvent.EventType.TURNEDFACEUP, getId(), source, playerId));
            return true;
        }
        return false;
    }

    @Override
    public boolean turnFaceDown(Ability source, Game game, UUID playerId) {
        GameEvent event = GameEvent.getEvent(GameEvent.EventType.TURNFACEDOWN, getId(), source, playerId);
        if (!game.replaceEvent(event)) {
            setFaceDown(true, game);
            game.fireEvent(GameEvent.getEvent(GameEvent.EventType.TURNEDFACEDOWN, getId(), source, playerId));
            return true;
        }
        return false;
    }

    @Override
    public boolean isTransformable() {
        return this.transformable;
    }

    @Override
    public void setTransformable(boolean transformable) {
        this.transformable = transformable;
    }

    @Override
    public final Card getSecondCardFace() {
        // init second side card on first call
        if (secondSideCardClazz == null && secondSideCard == null) {
            return null;
        }

        if (secondSideCard != null) {
            return secondSideCard;
        }

        // must be non strict search in any sets, not one set
        // example: if set contains only one card side
        // method used in cards database creating, so can't use repository here
        ExpansionSet.SetCardInfo info = Sets.findCardByClass(secondSideCardClazz, expansionSetCode);
        if (info == null) {
            return null;
        }
        secondSideCard = createCard(secondSideCardClazz, new CardSetInfo(info.getName(), expansionSetCode, info.getCardNumber(), info.getRarity(), info.getGraphicInfo()));
        return secondSideCard;
    }

    @Override
    public boolean isNightCard() {
        return this.nightCard;
    }

    @Override
    public boolean isFlipCard() {
        return flipCard;
    }

    @Override
    public String getFlipCardName() {
        return flipCardName;
    }

    @Override
    public boolean getUsesVariousArt() {
        return usesVariousArt;
    }

    @Override
    public Counters getCounters(Game game) {
        return getCounters(game.getState());
    }

    @Override
    public Counters getCounters(GameState state) {
        return state.getCardState(this.objectId).getCounters();
    }

    /**
     * @return The controller if available otherwise the owner.
     */
    protected UUID getControllerOrOwner() {
        return ownerId;
    }

    @Override
    public boolean addCounters(Counter counter, Ability source, Game game) {
        return addCounters(counter, source, game, null, true);
    }

    @Override
    public boolean addCounters(Counter counter, Ability source, Game game, boolean isEffect) {
        return addCounters(counter, source, game, null, isEffect);
    }

    @Override
    public boolean addCounters(Counter counter, Ability source, Game game, List<UUID> appliedEffects) {
        return addCounters(counter, source, game, appliedEffects, true);
    }

    @Override
    public boolean addCounters(Counter counter, Ability source, Game game, List<UUID> appliedEffects, boolean isEffect) {
        boolean returnCode = true;
        GameEvent addingAllEvent = GameEvent.getEvent(GameEvent.EventType.ADD_COUNTERS, objectId, source, getControllerOrOwner(), counter.getName(), counter.getCount());
        addingAllEvent.setAppliedEffects(appliedEffects);
        addingAllEvent.setFlag(isEffect);
        if (!game.replaceEvent(addingAllEvent)) {
            int amount = addingAllEvent.getAmount();
            boolean isEffectFlag = addingAllEvent.getFlag();
            int finalAmount = amount;
            for (int i = 0; i < amount; i++) {
                Counter eventCounter = counter.copy();
                eventCounter.remove(eventCounter.getCount() - 1);
                GameEvent addingOneEvent = GameEvent.getEvent(GameEvent.EventType.ADD_COUNTER, objectId, source, getControllerOrOwner(), counter.getName(), 1);
                addingOneEvent.setAppliedEffects(appliedEffects);
                addingOneEvent.setFlag(isEffectFlag);
                if (!game.replaceEvent(addingOneEvent)) {
                    getCounters(game).addCounter(eventCounter);
                    GameEvent addedOneEvent = GameEvent.getEvent(GameEvent.EventType.COUNTER_ADDED, objectId, source, getControllerOrOwner(), counter.getName(), 1);
                    addedOneEvent.setFlag(addingOneEvent.getFlag());
                    game.fireEvent(addedOneEvent);
                } else {
                    finalAmount--;
                    returnCode = false;
                }
            }
            if (finalAmount > 0) {
                GameEvent addedAllEvent = GameEvent.getEvent(GameEvent.EventType.COUNTERS_ADDED, objectId, source, getControllerOrOwner(), counter.getName(), amount);
                addedAllEvent.setFlag(isEffectFlag);
                game.fireEvent(addedAllEvent);
            }
        } else {
            returnCode = false;
        }
        return returnCode;
    }

    @Override
    public void removeCounters(String name, int amount, Ability source, Game game) {
        int finalAmount = 0;
        for (int i = 0; i < amount; i++) {
            if (!getCounters(game).removeCounter(name, 1)) {
                break;
            }
            GameEvent event = GameEvent.getEvent(GameEvent.EventType.COUNTER_REMOVED, objectId, source, getControllerOrOwner());
            event.setData(name);
            game.fireEvent(event);
            finalAmount++;
        }
        GameEvent event = GameEvent.getEvent(GameEvent.EventType.COUNTERS_REMOVED, objectId, source, getControllerOrOwner());
        event.setData(name);
        event.setAmount(finalAmount);
        game.fireEvent(event);
    }

    @Override
    public void removeCounters(Counter counter, Ability source, Game game) {
        if (counter != null) {
            removeCounters(counter.getName(), counter.getCount(), source, game);
        }
    }

    @Override
    public String getLogName() {
        if (name.isEmpty()) {
            return GameLog.getNeutralColoredText(EmptyNames.FACE_DOWN_CREATURE.toString());
        } else {
            return GameLog.getColoredObjectIdName(this);
        }
    }

    @Override
    public Card getMainCard() {
        return this;
    }

    @Override
    public FilterMana getColorIdentity() {
        FilterMana mana = new FilterMana();
        mana.setBlack(getManaCost().getText().matches(regexBlack));
        mana.setBlue(getManaCost().getText().matches(regexBlue));
        mana.setGreen(getManaCost().getText().matches(regexGreen));
        mana.setRed(getManaCost().getText().matches(regexRed));
        mana.setWhite(getManaCost().getText().matches(regexWhite));

        for (String rule : getRules()) {
            rule = rule.replaceAll("(?i)<i.*?</i>", ""); // Ignoring reminder text in italic
            if (!mana.isBlack() && (rule.matches(regexBlack) || this.color.isBlack())) {
                mana.setBlack(true);
            }
            if (!mana.isBlue() && (rule.matches(regexBlue) || this.color.isBlue())) {
                mana.setBlue(true);
            }
            if (!mana.isGreen() && (rule.matches(regexGreen) || this.color.isGreen())) {
                mana.setGreen(true);
            }
            if (!mana.isRed() && (rule.matches(regexRed) || this.color.isRed())) {
                mana.setRed(true);
            }
            if (!mana.isWhite() && (rule.matches(regexWhite) || this.color.isWhite())) {
                mana.setWhite(true);
            }
        }
        if (isTransformable()) {
            Card secondCard = getSecondCardFace();
            ObjectColor color = secondCard.getColor(null);
            mana.setBlack(mana.isBlack() || color.isBlack());
            mana.setGreen(mana.isGreen() || color.isGreen());
            mana.setRed(mana.isRed() || color.isRed());
            mana.setBlue(mana.isBlue() || color.isBlue());
            mana.setWhite(mana.isWhite() || color.isWhite());
            for (String rule : secondCard.getRules()) {
                rule = rule.replaceAll("(?i)<i.*?</i>", ""); // Ignoring reminder text in italic
                if (!mana.isBlack() && rule.matches(regexBlack)) {
                    mana.setBlack(true);
                }
                if (!mana.isBlue() && rule.matches(regexBlue)) {
                    mana.setBlue(true);
                }
                if (!mana.isGreen() && rule.matches(regexGreen)) {
                    mana.setGreen(true);
                }
                if (!mana.isRed() && rule.matches(regexRed)) {
                    mana.setRed(true);
                }
                if (!mana.isWhite() && rule.matches(regexWhite)) {
                    mana.setWhite(true);
                }
            }
        }

        return mana;
    }

    @Override
    public void setZone(Zone zone, Game game) {
        game.setZone(getId(), zone);
    }

    @Override
    public void setSpellAbility(SpellAbility ability) {
        spellAbility = ability;
    }

    @Override
    public List<UUID> getAttachments() {
        return attachments;
    }

    @Override
    public boolean addAttachment(UUID permanentId, Ability source, Game game) {
        if (!this.attachments.contains(permanentId)) {
            Permanent attachment = game.getPermanent(permanentId);
            if (attachment == null) {
                attachment = game.getPermanentEntering(permanentId);
            }
            if (attachment != null) {
                if (!game.replaceEvent(new AttachEvent(objectId, attachment, source))) {
                    this.attachments.add(permanentId);
                    attachment.attachTo(objectId, source, game);
                    game.fireEvent(new AttachedEvent(objectId, attachment, source));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeAttachment(UUID permanentId, Ability source, Game game) {
        if (this.attachments.contains(permanentId)) {
            Permanent attachment = game.getPermanent(permanentId);
            if (attachment != null) {
                attachment.unattach(game);
            }
            if (!game.replaceEvent(new UnattachEvent(objectId, permanentId, attachment, source))) {
                this.attachments.remove(permanentId);
                game.fireEvent(new UnattachedEvent(objectId, permanentId, attachment, source));
                return true;
            }
        }
        return false;
    }
}
