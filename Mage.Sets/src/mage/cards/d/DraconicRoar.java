package mage.cards.d;

import mage.abilities.Ability;
import mage.abilities.costs.CostAdjuster;
import mage.abilities.costs.common.RevealTargetFromHandCost;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.DamageTargetEffect;
import mage.abilities.effects.common.InfoEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.filter.FilterCard;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.common.TargetCardInHand;
import mage.target.common.TargetCreaturePermanent;
import mage.watchers.common.DragonOnTheBattlefieldWhileSpellWasCastWatcher;

import java.util.UUID;

/**
 * @author LevelX2
 */
public final class DraconicRoar extends CardImpl {

    public DraconicRoar(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{1}{R}");

        // As an additional cost to cast Draconic Roar, you may reveal a Dragon card from your hand.
        this.getSpellAbility().addEffect(new InfoEffect("as an additional cost to cast this spell, you may reveal a Dragon card from your hand"));
        this.getSpellAbility().setCostAdjuster(DraconicRoarAdjuster.instance);

        // Draconic Roar deals 3 damage to target creature. If you revealed a Dragon card or controlled a Dragon as you cast Draconic Roar, Draconic Roar deals 3 damage to that creature's controller.
        this.getSpellAbility().addEffect(new DamageTargetEffect(3));
        this.getSpellAbility().addTarget(new TargetCreaturePermanent());
        this.getSpellAbility().addEffect(new DraconicRoarEffect());
        this.getSpellAbility().addWatcher(new DragonOnTheBattlefieldWhileSpellWasCastWatcher());
    }

    public DraconicRoar(final DraconicRoar card) {
        super(card);
    }

    @Override
    public DraconicRoar copy() {
        return new DraconicRoar(this);
    }
}

enum DraconicRoarAdjuster implements CostAdjuster {
    instance;

    private static final FilterCard filter = new FilterCard("a Dragon card from your hand (you don't have to)");

    static {
        filter.add(SubType.DRAGON.getPredicate());
    }

    @Override
    public void adjustCosts(Ability ability, Game game) {
        Player controller = game.getPlayer(ability.getControllerId());
        if (controller != null) {
            if (controller.getHand().count(filter, game) > 0) {
                ability.addCost(new RevealTargetFromHandCost(new TargetCardInHand(0, 1, filter)));
            }
        }
    }
}

class DraconicRoarEffect extends OneShotEffect {

    public DraconicRoarEffect() {
        super(Outcome.Benefit);
        this.staticText = "If you revealed a Dragon card or controlled a Dragon as you cast {this}, {this} deals 3 damage to that creature's controller";
    }

    public DraconicRoarEffect(final DraconicRoarEffect effect) {
        super(effect);
    }

    @Override
    public DraconicRoarEffect copy() {
        return new DraconicRoarEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller != null) {
            DragonOnTheBattlefieldWhileSpellWasCastWatcher watcher = game.getState().getWatcher(DragonOnTheBattlefieldWhileSpellWasCastWatcher.class);
            if (watcher != null && watcher.castWithConditionTrue(source.getId())) {
                Permanent permanent = getTargetPointer().getFirstTargetPermanentOrLKI(game, source);
                if (permanent != null) {
                    Player player = game.getPlayer(permanent.getControllerId());
                    if (player != null) {
                        player.damage(3, source.getSourceId(), source, game);
                    }
                }
            }
            return true;
        }
        return false;
    }
}

