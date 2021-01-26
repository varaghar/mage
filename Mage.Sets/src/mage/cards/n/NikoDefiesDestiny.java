package mage.cards.n;

import mage.ConditionalMana;
import mage.MageObject;
import mage.Mana;
import mage.abilities.Ability;
import mage.abilities.SpellAbility;
import mage.abilities.common.SagaAbility;
import mage.abilities.condition.Condition;
import mage.abilities.costs.Cost;
import mage.abilities.dynamicvalue.DynamicValue;
import mage.abilities.effects.Effect;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.ReturnFromGraveyardToHandTargetEffect;
import mage.abilities.effects.mana.AddConditionalManaEffect;
import mage.abilities.keyword.ForetellAbility;
import mage.abilities.mana.builder.ConditionalManaBuilder;
import mage.abilities.mana.conditional.ManaCondition;
import mage.cards.*;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SagaChapter;
import mage.constants.SubType;
import mage.filter.FilterCard;
import mage.filter.predicate.mageobject.AbilityPredicate;
import mage.filter.predicate.other.OwnerIdPredicate;
import mage.game.ExileZone;
import mage.game.Game;
import mage.players.Player;
import mage.target.common.TargetCardInYourGraveyard;
import mage.util.CardUtil;

import java.util.Collection;
import java.util.UUID;

/**
 *
 * @author varaghar
 */
public final class NikoDefiesDestiny extends CardImpl {

    private static final FilterCard filter = new FilterCard("card with foretell from your graveyard");

    static {
        filter.add(
            new AbilityPredicate(ForetellAbility.class)
        );
    }

    public NikoDefiesDestiny(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ENCHANTMENT}, "{1}{U}{W}");

        this.subtype.add(SubType.SAGA);

        // (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
        SagaAbility sagaAbility = new SagaAbility(this, SagaChapter.CHAPTER_III);

        //  I — You gain 2 life for each foretold card you own in exile.
        sagaAbility.addChapterEffect(this, SagaChapter.CHAPTER_I, new NikoDefiesDestinyHealingEffect());

        //II — Add {W}{U}. Spend this mana only to foretell cards or cast spells that have foretell.
        sagaAbility.addChapterEffect(this, SagaChapter.CHAPTER_II, new AddConditionalManaEffect(
                new Mana(1, 1, 0, 0, 0, 0, 0, 0), new NikoDefiesDestinyManaBuilder()));

        // III — Return target card with foretell from your graveyard to your hand.
        sagaAbility.addChapterEffect(
                this, SagaChapter.CHAPTER_III, SagaChapter.CHAPTER_III,
                new ReturnFromGraveyardToHandTargetEffect()
                        .setText("Return target card with foretell from your graveyard to your hand"),
                new TargetCardInYourGraveyard(filter)
        );

        this.addAbility(sagaAbility);
    }

    private NikoDefiesDestiny(final NikoDefiesDestiny card) {
        super(card);
    }

    @Override
    public NikoDefiesDestiny copy() {
        return new NikoDefiesDestiny(this);
    }
}

class NikoDefiesDestinyHealingEffect extends OneShotEffect {

    public NikoDefiesDestinyHealingEffect() {
        super(Outcome.GainLife);
        staticText = "You gain 2 life for each foretold card you own in exile";
    }

    private NikoDefiesDestinyHealingEffect(final NikoDefiesDestinyHealingEffect effect) {
        super(effect);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        DynamicValue value = new ControlledForetoldCardsInExileValue();
        if (controller != null) {
            controller.gainLife(value.calculate(game, source, this) * 2, game, source);
            return true;
        }
        return false;
    }

    @Override
    public NikoDefiesDestinyHealingEffect copy() {
        return new NikoDefiesDestinyHealingEffect(this);
    }

}

class ControlledForetoldCardsInExileValue implements DynamicValue {

    @Override
    public int calculate(Game game, Ability sourceAbility, Effect effect) {
        Collection<ExileZone> exileZones = game.getState().getExile().getExileZones();
        Cards cardsForetoldInExileZones = new CardsImpl();
        FilterCard filter = new FilterCard();
        filter.add(new OwnerIdPredicate(sourceAbility.getControllerId()));
        filter.add(new AbilityPredicate(ForetellAbility.class));
        for (ExileZone exile : exileZones) {
            for (Card card : exile.getCards(filter, game)) {
                // verify that the card is actually Foretold
                UUID exileId = CardUtil.getExileZoneId(card.getId().toString() + "foretellAbility", game);
                if (game.getState().getExile().getExileZone(exileId) != null) {
                    cardsForetoldInExileZones.add(card);
                }
            }
        }

        return cardsForetoldInExileZones.size();
    }

    @Override
    public DynamicValue copy() {
        return new ControlledForetoldCardsInExileValue();
    }

    @Override
    public String getMessage() {
        return "";
    }
}

class NikoDefiesDestinyManaBuilder extends ConditionalManaBuilder {

    @Override
    public ConditionalMana build(Object... options) {
        return new NikoDefiesDestinyConditionalMana(this.mana);
    }

    @Override
    public String getRule() {
        return "Spend this mana only to foretell cards or cast spells that have foretell.";
    }
}

class NikoDefiesDestinyConditionalMana extends ConditionalMana {

    NikoDefiesDestinyConditionalMana(Mana mana) {
        super(mana);
        staticText = "Spend this mana only to foretell cards or cast spells that have foretell.";
        addCondition(new NikoDefiesDestinyManaCondition());
    }
}

class NikoDefiesDestinyManaCondition extends ManaCondition implements Condition {

    @Override
    public boolean apply(Game game, Ability source) {
        if (source instanceof SpellAbility) {
            MageObject object = game.getObject(source.getSourceId());
            return object != null && object.getAbilities().stream().anyMatch(a -> a instanceof ForetellAbility);
        }

        return source instanceof ForetellAbility;
    }

    @Override
    public boolean apply(Game game, Ability source, UUID originalId, Cost costsToPay) {
        return apply(game, source);
    }
}

