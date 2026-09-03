# Facebook OAuth Onboarding Guide

This document outlines the step-by-step onboarding process for connecting **Hugo Post** to a Facebook Page via Meta Graph API.

---

## 1. Overview & Required Permissions

To enable automated posting to a Facebook Page, the application requires:
- **Meta Developer App** registered on [developers.facebook.com](https://developers.facebook.com/)
- **Permissions**:
  - `pages_show_list`: Allows retrieving pages managed by the user.
  - `pages_read_engagement`: Required for reading page metadata.
  - `pages_manage_posts`: Allows publishing posts to the page feed.

---

## 2. Meta App Setup

1. Log in to [Meta for Developers](https://developers.facebook.com/) with the Facebook account that manages the target Page.
2. Go to **My Apps** > **Create App**.
3. Select **Business** or **Other** (Use case: *Manage business assets / Publish posts*).
4. Note your **App ID** and **App Secret** from **Settings > Basic**.

---

## 3. Obtaining a Permanent Page Access Token

Follow these steps to generate a Never-Expiring Page Access Token for your managed Facebook Page:

### Step 3.1: Generate Short-Lived User Access Token
1. Open the [Meta Graph API Explorer](https://developers.facebook.com/tools/explorer/).
2. Select your Meta App in the **Meta App** dropdown.
3. Under **User or Page**, select **User Token**.
4. Add the following permissions:
   - `pages_show_list`
   - `pages_read_engagement`
   - `pages_manage_posts`
5. Click **Generate Access Token** and approve the permissions.
6. Copy the resulting `SHORT_LIVED_USER_TOKEN`.

### Step 3.2: Exchange for Long-Lived User Access Token (60 days)
Execute the following HTTP `GET` request (via curl or browser):

```bash
curl -X GET "https://graph.facebook.com/v19.0/oauth/access_token?\
grant_type=fb_exchange_token&\
client_id={APP_ID}&\
client_secret={APP_SECRET}&\
fb_exchange_token={SHORT_LIVED_USER_TOKEN}"
```

Response will return the `LONG_LIVED_USER_TOKEN`:
```json
{
  "access_token": "LONG_LIVED_USER_TOKEN",
  "token_type": "bearer"
}
```

### Step 3.3: Retrieve Never-Expiring Page Access Token
Use the `LONG_LIVED_USER_TOKEN` to query your managed pages:

```bash
curl -X GET "https://graph.facebook.com/v19.0/me/accounts?access_token={LONG_LIVED_USER_TOKEN}"
```

Response:
```json
{
  "data": [
    {
      "access_token": "PAGE_ACCESS_TOKEN",
      "category": "Community",
      "name": "My Facebook Page",
      "id": "123456789012345",
      "tasks": ["ANALYZE", "ADVERTISE", "MODERATE", "CREATE_CONTENT", "MANAGE"]
    }
  ]
}
```

- Extract the target Page ID (`id`) and Page Access Token (`access_token`).
- **Note**: A Page Access Token derived from a Long-Lived User Token does **not expire** unless the user changes their password or revokes the app permissions.

---

## 4. Configuring `~/.hugopost`

Create or update the configuration file at `~/.hugopost` in your home directory:

```properties
# Hugo Post - Facebook Configuration
facebook.app_id=YOUR_FB_APP_ID
facebook.page_id=YOUR_FB_PAGE_ID
facebook.access_token=YOUR_PAGE_ACCESS_TOKEN
```

Alternatively, if using YAML format:
```yaml
facebook:
  app_id: "YOUR_FB_APP_ID"
  page_id: "YOUR_FB_PAGE_ID"
  access_token: "YOUR_PAGE_ACCESS_TOKEN"
```

---

## 5. Verification & Testing

Verify your setup by running a test post curl command:

```bash
curl -X POST "https://graph.facebook.com/v19.0/{PAGE_ID}/feed" \
     -d "message=Hello from Hugo Post!" \
     -d "access_token={PAGE_ACCESS_TOKEN}"
```

If successful, Meta returns `{ "id": "{PAGE_ID}_{POST_ID}" }`.


