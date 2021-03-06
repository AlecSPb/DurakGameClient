package com.yan.durak.layouting.pile.impl;

import com.yan.durak.gamelogic.cards.Card;
import com.yan.durak.layouting.CardsLayoutSlot;
import com.yan.durak.layouting.impl.PlayerCardsLayouter;
import com.yan.durak.layouting.pile.BasePileLayouter;
import com.yan.durak.models.PileModel;
import com.yan.durak.nodes.CardNode;
import com.yan.durak.services.CardNodesManagerService;
import com.yan.durak.services.PileManagerService;
import com.yan.durak.session.GameInfo;
import com.yan.durak.session.states.BaseDraggableState;
import com.yan.durak.session.states.IActivePlayerState;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.TweenManager;
import glengine.yan.glengine.service.ServiceLocator;

/**
 * Created by ybra on 20/04/15.
 */
public class BottomPlayerPileLayouter extends BasePileLayouter {


    private final PlayerCardsLayouter mPlayerCardsLayouter;

    public BottomPlayerPileLayouter(final PileManagerService pileManager, final CardNodesManagerService cardNodesManager,
                                    final TweenManager tweenManager, final PileModel boundPile) {
        super(cardNodesManager, tweenManager, boundPile);

        //init player cards layouter , assuming the entire deck can be in his hands
        mPlayerCardsLayouter = new PlayerCardsLayouter(pileManager.getAllCards().size());
    }


    /**
     * Initializes positions and all needed values for layouting
     */
    @Override
    public void init(final float sceneWidth, final float sceneHeight) {

        //init the player cards layouter
        mPlayerCardsLayouter.init(mCardNodesManager.getCardNodeOriginalWidth(), mCardNodesManager.getCardNodeOriginalHeight(),
                //maximum available width
                sceneWidth,
                //base x position ( center )
                sceneWidth / 2,
                //base y position
                sceneHeight * 0.93f);
    }


    @Override
    public void layout() {

        //by default every layouting will be animated
        float animationDuration = CARD_MOVEMENT_ANIMATION_DURATION;

        //cache the state
        final IActivePlayerState activePlayerState = ServiceLocator.locateService(GameInfo.class).getActivePlayerState();

        //each state will be handled differently
        switch (activePlayerState.getStateDefinition()) {
            case REQUEST_THROW_IN:
            case REQUEST_CARD_FOR_ATTACK:
            case REQUEST_RETALIATION:

                //make all card nodes touchable again
//                activateBottomPile();

                //dragging state alters the expansion level of the layouter
                //as well as changing the animationDuration of animation
                animationDuration = handleDraggingState(animationDuration, (BaseDraggableState) activePlayerState);
                break;
            default:

                //make all card nodes not touchable
//                deactivateBottomPile();
        }

        //the actual layouting is using underlying card layouter
        //which layouts slots
        layoutUsingSlots(animationDuration);

        //when player is not active we want the layouting to be compact
        mPlayerCardsLayouter.adjustExpansionLevel(PlayerCardsLayouter.ExpansionLevelPreset.COMPACT);
    }

    private void activateBottomPile() {
        //make all cards in player hand enabled
        final CardNodesManagerService cardNodeManager = ServiceLocator.locateService(CardNodesManagerService.class);
        for (final Card card : getBoundpile().getCardsInPile()) {
            final CardNode nodeToDisable = cardNodeManager.getCardNodeForCard(card);
            cardNodeManager.enableCardNode(nodeToDisable);
        }
    }

    private void deactivateBottomPile() {
        //make all cards in player hand disabled
        final CardNodesManagerService cardNodeManager = ServiceLocator.locateService(CardNodesManagerService.class);
        for (final Card card : getBoundpile().getCardsInPile()) {
            final CardNode nodeToDisable = cardNodeManager.getCardNodeForCard(card);
            cardNodeManager.disableCardNode(nodeToDisable);
        }
    }

    private float handleDraggingState(float duration, final BaseDraggableState activePlayerState) {
        //Both states allow to drag a card
        final BaseDraggableState draggableState = activePlayerState;
        if (draggableState.isDragging()) {
            //adjust expansion level by dragging distance
            mPlayerCardsLayouter.adjustExpansionLevel(draggableState.getDraggingCardDistanceFromPileField());

            //when dragged card distance begins to shrink , we want cards in player
            //hand to be hidden faster
            duration *= draggableState.getDraggingCardDistanceFromPileField();

        } else {
            //when player not dragging we want fully expanded hand
            mPlayerCardsLayouter.adjustExpansionLevel(PlayerCardsLayouter.ExpansionLevelPreset.EXPANDED);
        }
        return duration;
    }

    private void layoutUsingSlots(final float duration) {

        //update layouter to recalculate positions
        final int cardsInPileAmount = getBoundpile().getCardsInPile().size();
        mPlayerCardsLayouter.setActiveSlotsAmount(cardsInPileAmount);

        CardsLayoutSlot slot;
        CardNode cardNode;
        int slotPosition = 0;
        final float endAlpha = 1f;
        final Timeline tl = Timeline.createSequence().beginParallel();
        for (final Card card : mBoundpile.getCardsInPile()) {
            cardNode = mCardNodesManager.getCardNodeForCard(card);

            slot = mPlayerCardsLayouter.getSlotAtPosition(slotPosition);

            //important to update sorting layer
            cardNode.setSortingLayer(slot.getSortingLayer());

            //as it is the pile of the active player we want all cards to be visible
            cardNode.useFrontTextureRegion();

            //we need card to move instantly so we don't want previous animation to continue
            mTweenManager.killTarget(cardNode);

            addAnimationToTimelineForCardNode(tl, cardNode, slot.getPosition().getX(), slot.getPosition().getY(),
                    slot.getRotation(), mCardNodesManager.getCardNodeOriginalWidth(), mCardNodesManager.getCardNodeOriginalHeight(), endAlpha, duration);
            slotPosition++;
        }
        tl.start(mTweenManager);
    }
}
