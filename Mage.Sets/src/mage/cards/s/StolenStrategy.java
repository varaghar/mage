package mage.cards.s;

import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.common.BeginningOfUpkeepTriggeredAbility;
import mage.abilities.effects.AsThoughEffectImpl;
import mage.abilities.effects.OneShotEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.*;
import mage.game.ExileZone;
import mage.game.Game;
import mage.players.Player;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author TheElk801
 */
public final class StolenStrategy extends CardImpl {

    public StolenStrategy(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ENCHANTMENT}, "{4}{R}");

        // At the beginning of your upkeep, exile the top card of each opponent's library. Until end of turn, you may cast nonland cards from among those exiled cards, and you may spend mana as though it were mana of any color to cast those spells.
        this.addAbility(new BeginningOfUpkeepTriggeredAbility(new StolenStrategyEffect(), TargetController.YOU, false));
    }

    public StolenStrategy(final StolenStrategy card) {
        super(card);
    }

    @Override
    public StolenStrategy copy() {
        return new StolenStrategy(this);
    }
}

class StolenStrategyEffect extends OneShotEffect {

    public StolenStrategyEffect() {
        super(Outcome.PutCreatureInPlay);
        this.staticText = "exile the top card of each opponent's library. "
                + "Until end of turn, you may cast nonland cards from among those exiled cards, "
                + "and you may spend mana as though it were mana of any color to cast those spells";
    }

    public StolenStrategyEffect(final StolenStrategyEffect effect) {
        super(effect);
    }

    @Override
    public StolenStrategyEffect copy() {
        return new StolenStrategyEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        for (UUID playerId : game.getState().getPlayersInRange(controller.getId(), game)) {
            if (!controller.hasOpponent(playerId, game)) {
                continue;
            }
            Player damagedPlayer = game.getPlayer(playerId);
            if (damagedPlayer == null) {
                continue;
            }
            MageObject sourceObject = game.getObject(source.getSourceId());
            UUID exileId = CardUtil.getCardExileZoneId(game, source);
            Card card = damagedPlayer.getLibrary().getFromTop(game);
            if (card != null && sourceObject != null) {
                // move card to exile
                controller.moveCardToExileWithInfo(card, exileId, sourceObject.getIdName(), source, game, Zone.LIBRARY, true);
                // Add effects only if the card has a spellAbility (e.g. not for lands).
                if (!card.isLand() && card.getSpellAbility() != null) {
                    // allow to cast the card
                    // and you may spend mana as though it were mana of any color to cast it
                    CardUtil.makeCardPlayableAndSpendManaAsAnyColor(game, source, card, Duration.EndOfTurn);
                }
            }
        }
        return true;
    }
}

class StolenStrategyCastFromExileEffect extends AsThoughEffectImpl {

    private final UUID cardId;
    private final UUID exileId;

    public StolenStrategyCastFromExileEffect(UUID cardId, UUID exileId) {
        super(AsThoughEffectType.PLAY_FROM_NOT_OWN_HAND_ZONE, Duration.EndOfTurn, Outcome.Benefit);
        staticText = "Until end of turn, you may cast that card and you may spend mana as though it were mana of any color to cast it";
        this.cardId = cardId;
        this.exileId = exileId;
    }

    public StolenStrategyCastFromExileEffect(final StolenStrategyCastFromExileEffect effect) {
        super(effect);
        this.cardId = effect.cardId;
        this.exileId = effect.exileId;
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return true;
    }

    @Override
    public StolenStrategyCastFromExileEffect copy() {
        return new StolenStrategyCastFromExileEffect(this);
    }

    @Override
    public boolean applies(UUID sourceId, Ability source, UUID affectedControllerId, Game game) {
        if (sourceId.equals(cardId) && source.isControlledBy(affectedControllerId)) {
            ExileZone exileZone = game.getState().getExile().getExileZone(exileId);
            return exileZone != null && exileZone.contains(cardId);
        }
        return false;
    }
}
