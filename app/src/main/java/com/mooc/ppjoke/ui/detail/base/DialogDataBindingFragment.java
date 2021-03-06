package com.mooc.ppjoke.ui.detail.base;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.kunminx.architecture.BaseApplication;
import com.kunminx.architecture.ui.page.DataBindingConfig;
import com.mooc.libarchitecture.R;
import com.mooc.libcommon.utils.PixUtils;
import com.mooc.libcommon.view.ViewHelper;

public abstract class DialogDataBindingFragment extends AppCompatDialogFragment {

	private static final Handler HANDLER = new Handler();
	protected AppCompatActivity mActivity;
	protected boolean mAnimationLoaded;
	private ViewModelProvider mFragmentProvider;
	private ViewModelProvider mActivityProvider;
	private ViewModelProvider.Factory mFactory;
	private TextView mTvStrictModeTip;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		mActivity = (AppCompatActivity) context;
	}

	protected abstract void initViewModel();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initViewModel();
	}

	protected abstract DataBindingConfig getDataBindingConfig();


	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		DataBindingConfig dataBindingConfig = getDataBindingConfig();

		//TODO tip: DataBinding 严格模式：
		// 将 DataBinding 实例限制于 base 页面中，默认不向子类暴露，
		// 通过这样的方式，来彻底解决 视图调用的一致性问题，
		// 如此，视图刷新的安全性将和基于函数式编程的 Jetpack Compose 持平。

		// 如果这样说还不理解的话，详见 https://xiaozhuanlan.com/topic/9816742350 和 https://xiaozhuanlan.com/topic/2356748910
		// TODO tip 设置DialogFragment的Window属性
		Window window = getDialog().getWindow();
		window.setWindowAnimations(0);
		ViewDataBinding binding = DataBindingUtil.inflate(inflater, dataBindingConfig.getLayout(), ((ViewGroup) window.findViewById(android.R.id.content)), false);

		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

		ViewHelper.setViewOutline(binding.getRoot(), PixUtils.dp2px(10), ViewHelper.RADIUS_TOP);

		binding.setLifecycleOwner(this);
		binding.setVariable(dataBindingConfig.getVmVariableId(), dataBindingConfig.getStateViewModel());
		SparseArray bindingParams = dataBindingConfig.getBindingParams();
		for (int i = 0, length = bindingParams.size(); i < length; i++) {
			binding.setVariable(bindingParams.keyAt(i), bindingParams.valueAt(i));
		}
		binding.getRoot().post(this::postMethod);
		return binding.getRoot();
	}

	protected void postMethod() {

	}

	@Nullable
	@Override
	public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
		//TODO 错开动画转场与 UI 刷新的时机，避免掉帧卡顿的现象
		HANDLER.postDelayed(() -> {
			if (!mAnimationLoaded) {
				mAnimationLoaded = true;
				loadInitData();
			}
		}, 280);
		return super.onCreateAnimation(transit, enter, nextAnim);
	}

	protected void loadInitData() {

	}

	public boolean isDebug() {
		return mActivity.getApplicationContext().getApplicationInfo() != null &&
				(mActivity.getApplicationContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
	}

	protected void showLongToast(String text) {
		Toast.makeText(mActivity.getApplicationContext(), text, Toast.LENGTH_LONG).show();
	}

	protected void showShortToast(String text) {
		Toast.makeText(mActivity.getApplicationContext(), text, Toast.LENGTH_SHORT).show();
	}

	protected void showLongToast(int stringRes) {
		showLongToast(mActivity.getApplicationContext().getString(stringRes));
	}

	protected void showShortToast(int stringRes) {
		showShortToast(mActivity.getApplicationContext().getString(stringRes));
	}

	protected <T extends ViewModel> T getFragmentViewModel(@NonNull Class<T> modelClass) {
		if (mFragmentProvider == null) {
			mFragmentProvider = new ViewModelProvider(this);
		}
		return mFragmentProvider.get(modelClass);
	}

	protected <T extends ViewModel> T getActivityViewModel(@NonNull Class<T> modelClass) {
		if (mActivityProvider == null) {
			mActivityProvider = new ViewModelProvider(mActivity);
		}
		return mActivityProvider.get(modelClass);
	}

	protected ViewModelProvider getAppViewModelProvider() {
		return new ViewModelProvider((BaseApplication) mActivity.getApplicationContext(),
				getAppFactory(mActivity));
	}

	private ViewModelProvider.Factory getAppFactory(Activity activity) {
		checkActivity(this);
		Application application = checkApplication(activity);
		if (mFactory == null) {
			mFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application);
		}
		return mFactory;
	}

	private Application checkApplication(Activity activity) {
		Application application = activity.getApplication();
		if (application == null) {
			throw new IllegalStateException("Your activity/fragment is not yet attached to "
					+ "Application. You can't request ViewModel before onCreate call.");
		}
		return application;
	}

	private void checkActivity(Fragment fragment) {
		Activity activity = fragment.getActivity();
		if (activity == null) {
			throw new IllegalStateException("Can't create ViewModelProvider for detached fragment");
		}
	}
}
