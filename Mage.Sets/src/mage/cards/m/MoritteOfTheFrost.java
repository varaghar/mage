package mage.cards.m;

import mage.MageInt;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldAbility;
import mage.abilities.effects.common.CopyPermanentEffect;
import mage.abilities.effects.common.counter.AddCountersSourceEffect;
import mage.abilities.keyword.ChangelingAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.counters.CounterType;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.util.functions.ApplyToPermanent;

import java.util.UUID;

/**
 * @author TheElk801
 */
public final class MoritteOfTheFrost extends CardImpl {

    public MoritteOfTheFrost(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{G}{U}{U}");

        this.addSuperType(SuperType.LEGENDARY);
        this.addSuperType(SuperType.SNOW);
        this.subtype.add(SubType.SHAPESHIFTER);
        this.power = new MageInt(0);
        this.toughness = new MageInt(0);

        // Changeling
        this.setIsAllCreatureTypes(true);
        this.addAbility(ChangelingAbility.getInstance());

        // You may have Moritte of the Frost enter the battlefield as a copy of a permanent you control, except it's legendary and snow in addition to its other types and, if it's a creature, it enters with two additional +1/+1 counters on it and has changeling.
        this.addAbility(new EntersBattlefieldAbility(new CopyPermanentEffect(
                StaticFilters.FILTER_CONTROLLED_PERMANENT, new MoritteOfTheFrostApplier()
        ).setText("as a copy of a permanent you control, except it's legendary and snow in addition to its other types " +
                "and, if it's a creature, it enters with two additional +1/+1 counters on it and has changeling."
        ), true));
    }

    private MoritteOfTheFrost(final MoritteOfTheFrost card) {
        super(card);
    }

    @Override
    public MoritteOfTheFrost copy() {
        return new MoritteOfTheFrost(this);
    }
}

class MoritteOfTheFrostApplier extends ApplyToPermanent {

    @Override
    public boolean apply(Game game, Permanent copyFromBlueprint, Ability source, UUID copyToObjectId) {
        return apply(game, (MageObject) copyFromBlueprint, source, copyToObjectId);
    }

    @Override
    public boolean apply(Game game, MageObject copyFromBlueprint, Ability source, UUID copyToObjectId) {
        copyFromBlueprint.addSuperType(SuperType.LEGENDARY);
        copyFromBlueprint.addSuperType(SuperType.SNOW);

        if (!isCopyOfCopy(source, copyToObjectId) && copyFromBlueprint.isCreature()) {
            copyFromBlueprint.setIsAllCreatureTypes(true);
            copyFromBlueprint.getAbilities().add(ChangelingAbility.getInstance());
            new AddCountersSourceEffect(
                    CounterType.P1P1.createInstance(2), false
            ).apply(game, source);
        }
        return true;
    }
}
