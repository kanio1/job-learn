#### 
                    Adrian Maciuc
                

                
        

        
            
                May 5, 2024
                    
                    8 min
            
        

    

                    
Everybody hates it when they go to an interview and they are given live coding challenge that is written intentionally to make you fail, so the interviewer can prove his &quot;superiority&quot;. Here are 12 playwright interview questions and answers, so you can have the advantage this time.

            

            
                

Questions were created with the idea to trick you, so don't be too hard on yourself if you don't get them right. Also, some answers have an explanation and even code examples. Scroll all the way to the bottom for that. Remember, this is functional code with real URLs used, so feel free to try the code out, copy paste it and see for yourself.

## 

## 1. The local environment

If you would run your tests on your local machine using your local environment that you need to setup and get it up and running, for example at http://localhost:3000/ , how would you handle the local environment setup during test runs?

Possible answers:

- Start up your local environment prior to running your tests
- Write a script in package.json to start local environment and run your tests
- Use webServer
            
                
#### Answer

                
                    
                        

                    
                
            
            All possible answers are valid but the recommended one is to use webServer config

see explanation at the end of the page...

        

## 2. The contain comparison

Other than the fact that one takes a `locator` and the other a `value`, what is the main difference between the followings: 

`await expect(locator).toContainText()`

and

`await expect(value).toContain()`

            
                
#### Answer

                
                    
                        

                    
                
            
            First one is an auto-retry assertion meaning that it will retry up to 5 seconds for the element to appear and the second will attempt to assert once.

        

## 3. The delayed load

At the URL below, text "Loading complete" will appear after 10 seconds. Our test is failing at the expect step with `Expected:&nbsp;visible , Received:&nbsp;hidden`. How could you fix this test ?

`test("The visible methods", async ({ page }) =&gt; {
  await page.goto("https://webdriveruniversity.com/Accordion/index.html");
  await expect(page.getByText("LOADING COMPLETE.")).toBeVisible()
});`
            
                
#### Answer

                
                    
                        
                    
                
            
            Default expect timeout is 5 seconds, if element is not visible within that time interval, test will fail. To fix the test we extend the timeout only for this step by adding a timeout above our known 10 seconds delay, like so: `await expect(page.getByText("LOADING COMPLETE.")).toBeVisible({ timeout: 12000 })`

        

## 4. The self-healing

Knowing that magento.softwaretestingboard.com has a menu item called "Sale" that upon clicking it will navigate to a new page. Complete the test below with writing a new step that will click on "Sale" menu option but create for that element a self-healing locator

`test("The self-healing", async ({ page }) =&gt; {
  await page.goto("https://magento.softwaretestingboard.com/");
  await page.getByLabel('Consent', { exact: true }).click()
});`
            
                
#### Answer

                
                    
                        
                    
                
            
            A self-healing locator is when you point multiple locators to the same element but you chain them together with the use of a helper method or with the OR operator. Using the or operator you can provide multiple locators that will point to the same element, in case one is broken due to app change it can try to use the other one

see code example at the end of the page...

        
## 5. The closed browser

What is this code going to do:

`test("The closed browser", async ({ page }) =&gt; {
  await page.goto("https://blog.martioli.com/");
  await page
    .getByRole('link', { name: 'About' })
    .click();
  expect(page).toHaveURL(/about/);
});`Possible answers:

- Test will fail because About is a button not a link
- Test will error out about target page
- Test will fail because of the argument passed to .toHaveURL()
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will error out about target page. Error:&nbsp;expect.toHaveURL:&nbsp;Target&nbsp;page,&nbsp;context&nbsp;or&nbsp;browser&nbsp;has&nbsp;been&nbsp;closed

see explanation at the end of the page...

        

## 6. The reference

Knowing that the element clicked on the second step is a backlink on a page, what is this code going to do:

`test("The reference", async ({ page }) =&gt; {
  await page.goto("https://blog.martioli.com/playwright-tips-and-tricks-1/");
  await page
    .locator('section').locator('p').locator('a').getByText('buy me a coffee').click()
  await expect(page).toHaveURL(/blog.martioli.com/)
});`Possible answers:

- Test will fail because 'buy me a coffee' is a link and we navigated away from martioli.com
- Test will pass
- Test will fail because you cannot mix locator().locator().locator()
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will pass, because clicking a backlink will generate an URL with a reference about our initial origin (blog.martioli.com)

see explanation at the end of the page...

        

## 7. The other locales

Modify the test below and have it run with a different locale, for example german. 

`test("The locale", async ({ page }) =&gt; {
  await page.goto("https://www.google.com/");
});`
            
                
#### Answer

                
                    
                        
                    
                
            
            You can modify configurations from within your test. Just add above the test `test.use({locale: 'de-DE'})`

        

## 8. The CSS properties

Given the code below how could you improve it for readability and simplicity.

`test("The css properties", async ({ page }) =&gt; {
  await page.goto("https://magento.softwaretestingboard.com/");
  await page.getByLabel('Consent', { exact: true }).click()
  
  const element = page.getByText("Shop New Yoga")
  const backgroundColor = await element.evaluate((el) =&gt; {
    return window.getComputedStyle(el).getPropertyValue('background-color');
  });
  expect(backgroundColor).toBe("rgb(25, 121, 195)")
});`
            
                
#### Answer

                
                    
                        
                    
                
            
            You don't need to call evaluate when you already have built-in method called toHaveCSS() . Remember also that `toHaveCSS()` has auto-retry, which is crucial if you wait for certain element properties to appear with delay.

see code example at the end of the page...

        

## 9. The long click

Given the code below, how would you modify the click step to simulate hold left mouse button for 3 seconds and then release mouse button. 

`test("The long click", async ({ page }) =&gt; {
  await page.goto("https://www.clickspeedtester.com/mouse-test/");
  await page.getByRole('link', { name: 'Second Clicker' }).click()
});`
            
                
#### Answer

                
                    
                        
                    
                
            
            Click can take optionally an object with property delay and value in milliseconds. Like so: `.click({delay: 3000})` pair this with an element and it will hold for 3 seconds on that element then release the mouse button

        

## 10. The force

The page has a data consent popup modal that you have to comply with. The "search" button is hidden behind this modal. Given the following code. What will it happen

`test("The force", async ({ page }) =&gt; {
  await page.goto("https://www.google.com/");
  
  // the search input field
  await page.locator('textarea').first().fill("martioli")
  // the search button
  await page.locator('[type=submit][name="btnK"]').last().click({ force: true })
});`Possible answers:

- Test will pass. I am using `{force:true}` , it will click the button and results will show behind the modal
- Test will pass, but no results shown behind the modal
- Test will fail, timeout, because it cannot click on the button behind the modal
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will pass, but no results shown behind the modal. `force:true` does not click if element is under another element, but it will also not error out (bug in playwright?). Its not the same for `fill()`. If you check the input, it had no problems in typing in the input field.

        
## 11. The baseUrl

I have my baseUrl set up in my config file.  What is this code going to do:

`test("The baseurl", async ({ page }) =&gt; {
  await page.goto('');
});`Possible answers:

- Test will fail. I forgot the `/` between the quotes
- Test will fail because `.goto()` needs double quotes
- Test will pass
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will pass, an empty string does the same as "/" . It will navigate to baseUrl configured in playwright.config.js

        
## 12. The workers

Given the below configuration about workers. If I run a suite of 100 tests, how would they run:

`module.exports = defineConfig({
  testDir: "./tests",
  workers: "5%",

  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
`Possible answers:

- Tests will run very slow with only 5% power of the workers
- Test will run on 5% of how many CPU cores you have.
- Test will not even start because you cannot set values in percentages for workers
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will run on 5% of how many CPU cores you have. For example if you have 4 cores, it means out of 100% 4 would mean each 25%. In our case 5 is under 25% so you will get just one worker

        Become a member for free 

Hit the clap button if you found this useful. Or even buy me a coffee if you want to motivate me even more.

Playwright interview questions that you are going to lovePlaywright interview questions for Mid and Senior level quality assurance automation engineersMartioliAdrian Maciuc
My blog has a members section. Its free to join, there is no spam and you get access to members only content. Become a member now and you will also be the first to receive in your email when I publish a new article.

### Content below is visible to members only

    
                
## This post is for subscribers only

            Subscribe now
            
Already have an account? Sign in
