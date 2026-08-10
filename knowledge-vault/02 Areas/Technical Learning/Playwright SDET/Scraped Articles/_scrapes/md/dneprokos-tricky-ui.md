Hello everyone,

Playwright is one of the most user-friendly test automation tools for beginners. Today we cover tricky UI scenarios:

- JavaScript Alerts
- IFrames
- Shadow DOM (open)
- File Download
- File Upload
- Tables

## Alerts

### Alert Box

Subscribe to the `dialog` event and call `dismiss`:

```javascript
page.on('dialog', async dialog => await dialog.dismiss());
```

### Confirm Box

Use `dialog.accept()` or `dialog.dismiss()`.

### Prompt Box

```javascript
page.on('dialog', async dialog => await dialog.accept('Test'));
```

## IFrames

Use `page.frameLocator()` to chain into nested frames:

```typescript
this.frameTop = page.frameLocator('frame[name="frame-top"]');
this.innerTopLeftFrame = this.frameTop.frameLocator('frame[name="frame-left"]');
return await this.innerTopLeftFrame.locator('body').innerText();
```

Unlike WebDriver, you don't switch in and out of frames.

## Shadow DOM

Shadow DOM encapsulates component markup. Access with component name in selector:

```typescript
return await this.page.locator('my-paragraph span').textContent();
return await this.page.locator('my-paragraph ul li').allInnerTexts();
```

## File Download

```typescript
const [download] = await Promise.all([
  this.page.waitForEvent('download'),
  this.page.click(`a[href="download/${expectedFileName}"]`)
]);
await download.saveAs(savePath);
```

In tests: resolve save path, call download method, assert file exists, clean up with `fs.unlinkSync`.

## File Upload

```typescript
await this.page.setInputFiles('#file-upload', filePath);
await this.page.getByRole('button', { name: 'Upload' }).click();
```

Verify with `getUploadedFileName()` reading `#uploaded-files`.

## Tables

Define a row model interface, locate rows, iterate cells by index:

```typescript
const tableRows = this.page.locator(this.tableRowsSelector);
const rowCount = await tableRows.count();
// for each row, read td cells by index into model
```

Sort by field if needed for deterministic assertions.

## Epilog

You don't need much code to solve UI challenges with Playwright. Even tricky cases are easier than they look. Repository with examples linked from the original article.

Alerts: page.on dialog dismiss accept accept with text Test for prompt box. IFrames frameLocator chain frame-top frame-left frame-middle frame-right frame-bottom get body innerText without WebDriver switchTo.

Shadow DOM my-paragraph span textContent and ul li allInnerTexts open shadow root selectors pierce encapsulation.

File download Promise.all waitForEvent download click href saveAs fs.existsSync assert unlinkSync cleanup.

File upload setInputFiles path getByRole Upload click getUploadedFileName textContent uploaded-files trim assert filename match.

Tables ExampleOneTableModel interface lastName firstName email due webSite row locator td index loop sortTableByField TableHeaderNames.Email optional sort for random table data.

Playwright Discord https://discord.com/servers/playwright-807756831384403968 community support for beginners.

Topics covered JavaScript Alerts IFrames Shadow Dom open File Download File Upload Tables movie epilogue pop culture references.

Kostiantyn Teltov January 2024 9 min read Medium Playwright stories beginners UI automation tricky scenarios Ukraine QA architect speaker.

May the force be with you epilog closing — easier than you think try Playwright for alerts frames shadow roots downloads uploads table parsing.

Demo repo linked at article end for hands-on practice with training site implementations each section references concrete page object methods async getFrameBottomText getInnerTopLeftFrameText patterns.
