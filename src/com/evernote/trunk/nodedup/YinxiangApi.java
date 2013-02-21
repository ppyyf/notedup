package com.evernote.trunk.nodedup;

import org.scribe.builder.api.EvernoteApi;
import org.scribe.model.Token;

public class YinxiangApi extends EvernoteApi {

  private static final String YINXIANG_URL = "https://app.yinxiang.com"; //$NON-NLS-1$

  @Override
  public String getRequestTokenEndpoint()
  {
    return YINXIANG_URL + "/oauth"; //$NON-NLS-1$
  }

  @Override
  public String getAccessTokenEndpoint()
  {
  return YINXIANG_URL + "/oauth"; //$NON-NLS-1$
  }

  @Override
  public String getAuthorizationUrl(Token requestToken)
  {
    return String.format(YINXIANG_URL + "/OAuth.action?oauth_token=%s", requestToken.getToken()); //$NON-NLS-1$
  }
}