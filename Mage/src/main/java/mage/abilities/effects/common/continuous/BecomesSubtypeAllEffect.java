/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mage.abilities.effects.common.continuous;

import mage.abilities.Ability;
import mage.abilities.effects.ContinuousEffectImpl;
import mage.constants.*;
import mage.filter.StaticFilters;
import mage.filter.common.FilterCreaturePermanent;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.util.SubTypeList;

/**
 * @author LevelX2
 */
public class BecomesSubtypeAllEffect extends ContinuousEffectImpl {

    private final SubTypeList subtypes = new SubTypeList();
    private final boolean loseOther; // loses other subtypes
    private final FilterCreaturePermanent filter;

    public BecomesSubtypeAllEffect(Duration duration, SubType subtype) {
        this(duration, new SubTypeList(subtype));
    }

    public BecomesSubtypeAllEffect(Duration duration, SubTypeList subtypes) {
        this(duration, subtypes, StaticFilters.FILTER_PERMANENT_CREATURE, true);
    }

    public BecomesSubtypeAllEffect(Duration duration, SubTypeList subtypes, FilterCreaturePermanent filter, boolean loseOther) {
        super(duration, Layer.TypeChangingEffects_4, SubLayer.NA, Outcome.Detriment);
        this.subtypes.addAll(subtypes);
        this.staticText = setText();
        this.loseOther = loseOther;
        this.filter = filter;
    }

    public BecomesSubtypeAllEffect(final BecomesSubtypeAllEffect effect) {
        super(effect);
        this.subtypes.addAll(effect.subtypes);
        this.loseOther = effect.loseOther;
        this.filter = effect.filter;
    }

    @Override
    public BecomesSubtypeAllEffect copy() {
        return new BecomesSubtypeAllEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        boolean flag = false;
        for (Permanent permanent : game.getBattlefield().getActivePermanents(filter, source.getControllerId(), source.getSourceId(), game)) {
            if (permanent == null) {
                continue;
            }
            flag = true;
            if (loseOther) {
                permanent.removeAllCreatureTypes(game);
            }
            for (SubType subtype : subtypes) {
                permanent.addSubType(game, subtype);
            }
        }
        if (!flag && duration == Duration.Custom) {
            discard();
        }
        return true;
    }

    private String setText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Target creature becomes that type");
        if (!duration.toString().isEmpty()
                && duration != Duration.EndOfGame) {
            sb.append(' ').append(duration.toString());
        }
        return sb.toString();
    }
}
