package mage.cards.s;

import mage.abilities.Ability;
import mage.abilities.common.ActivateAsSorceryActivatedAbility;
import mage.abilities.common.EntersBattlefieldTappedAbility;
import mage.abilities.costs.common.SacrificeSourceCost;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.common.CreateTokenEffect;
import mage.abilities.effects.common.continuous.BoostTargetEffect;
import mage.abilities.mana.GreenManaAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Zone;
import mage.filter.StaticFilters;
import mage.game.permanent.token.ElfToken;
import mage.target.TargetPermanent;

import java.util.UUID;

/**
 * @author TheElk801
 */
public final class SkemfarElderhall extends CardImpl {

    public SkemfarElderhall(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.LAND}, "");

        // Skemfar Elderhall enters the battlefield tapped.
        this.addAbility(new EntersBattlefieldTappedAbility());

        // {T}: Add {G}.
        this.addAbility(new GreenManaAbility());

        // {2}{B}{B}{G}, {T}, Sacrifice Skemfar Elderhall: Up to one target creature you don't control gets -2/-2 until end of turn. Create two 1/1 green Elf Warrior creature tokens. Activate this ability only any time you could cast a sorcery.
        Ability ability = new ActivateAsSorceryActivatedAbility(
                Zone.BATTLEFIELD,
                new BoostTargetEffect(-2, -2),
                new ManaCostsImpl<>("{2}{B}{B}{G}")
        );
        ability.addCost(new TapSourceCost());
        ability.addCost(new SacrificeSourceCost());
        ability.addEffect(new CreateTokenEffect(new ElfToken(), 2));
        ability.addTarget(new TargetPermanent(StaticFilters.FILTER_CREATURE_YOU_DONT_CONTROL));
        this.addAbility(ability);
    }

    private SkemfarElderhall(final SkemfarElderhall card) {
        super(card);
    }

    @Override
    public SkemfarElderhall copy() {
        return new SkemfarElderhall(this);
    }
}
