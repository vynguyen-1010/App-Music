package com.fhm.musicr.glide;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.ImageView;

import com.fhm.musicr.R;
import com.fhm.musicr.glide.palette.BitmapPaletteTarget;
import com.fhm.musicr.glide.palette.BitmapPaletteWrapper;
import com.fhm.musicr.util.PhonographColorUtil;


public abstract class ColoredTarget extends BitmapPaletteTarget {
    public ColoredTarget(ImageView view) {
        super(view);
    }

    @Override
    public void onLoadFailed(Drawable errorDrawable) {
        super.onLoadFailed(errorDrawable);
        onColorReady(getView().getContext().getResources().getColor(R.color.FlatOrange));
    }

    @Override
    public void onResourceReady(@NonNull BitmapPaletteWrapper resource, @Nullable com.bumptech.glide.request.transition.Transition<? super BitmapPaletteWrapper> transition) {
        super.onResourceReady(resource, transition);
        onColorReady(PhonographColorUtil.getColor(resource.getPalette(), getView().getContext().getResources().getColor(R.color.FlatOrange)/*getDefaultFooterColor()*/));
    }

  /*  protected int getDefaultFooterColor() {
        return ATHUtil.resolveColor(getView().getContext(), R.attr.defaultFooterColor);
    }

    protected int getAlbumArtistFooterColor() {
        return ATHUtil.resolveColor(getView().getContext(), R.attr.cardBackgroundColor);
    }*/

    public abstract void onColorReady(int color);
}
