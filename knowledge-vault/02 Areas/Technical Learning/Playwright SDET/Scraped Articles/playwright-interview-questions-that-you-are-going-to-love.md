#### 
                    Adrian Maciuc
                

                
        

        
            
                May 17, 2024
                    
                    6 min
            
        

    

                    
Playwright interview questions for Mid and Senior level quality assurance automation engineers 

            

            
                
Following my previous article about playwright interview questions, here are 9 playwright tricky questions that you don't want to fail

## 

## 1. The explicit waits

From testing search functionality point of view how would you improve the code below:

`test("The explicit waits", async ({ page }) =&gt; {
  await page.goto("https://blog.martioli.com/playwright-tips-and-tricks-2/")
  await page.getByText('Playwright tips and tricks #2').scrollIntoViewIfNeeded()
  await expect(page.getByText('Playwright tips and tricks #2')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Search this site' })).toBeVisible()
  await page.getByRole('button', { name: 'Search this site' }).click()
  await expect(page.frameLocator('iframe[title="portal-popup"]').getByPlaceholder('Search posts, tags and authors')).toBeVisible()
  await page.frameLocator('iframe[title="portal-popup"]').getByPlaceholder('Search posts, tags and authors').fill("Cypress")
  await expect(page.frameLocator('iframe[title="portal-popup"]').getByRole('heading', { name: 'Cypress' }).first()).toContainText("Cypress")
});`
            
                
#### Answer

                
                    
                        
                    
                
            
            
- Remove all the `toBeVisible()` expects
- Remove the `scrollIntoViewIfNeeded()`
- Store iframes in a constant for reuse and readability
- Use regex in your locators to be able to use partial text. Such as `/Search posts/` instead of `Search posts, tags and authors`see code and explanation at the end of the page...

        

## 2. The visible methods

What is this code going to do:

`test("The visible methods", async ({ page }) =&gt; {
  await page.goto("https://blog.martioli.com/");
  await expect(page.getByRole('link', { name: 'About' }).isVisible())
});`Possible answers:

- Test will fail because isVisible() is not a valid method that can be used
- Test will fail with an error about property&nbsp;'then'
- Test will pass
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will fail with an error . Error:&nbsp;expect:&nbsp;Property&nbsp;'then'&nbsp;not&nbsp;found 

see explanation at the end of the page...

        

## 3. The ninja click

Given the code below, what do you think it will happen

`test("The ninja", async ({ page }) =&gt; {
  await page.goto("https://www.clickspeedtester.com/mouse-test/");
  await page.getByRole('link', { name: 'Second Clicker' }).click({ trial: true })
  await page.waitForURL("**/clicks-per-second-test/")
})`Possible answers:

- Test will fail with error page.waitForURL:&nbsp;Test&nbsp;ended, because click was not performed
- Test will fail because `waitForURL()` argument is not in valid format
- Test will fail at click step, there is no such thing as `trial:true`
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will fail with error page.waitForURL:&nbsp;Test&nbsp;ended. With trial:true Playwright performs the&nbsp;actionability&nbsp;checks but skips the action of click. 

        

## 4. The you OK ?

Given the code below, what do you think it will happen 

`test("The you OK", async ({ page }) =&gt; {
  const response = await page.request.get('https://blog.martioli.com/');
  await expect(response).toBeOK();
})`Possible answers:

- Test will fail because there is no such thing as `toBeOK()`
- Test will fail because `page` does not have `request`
- Test will pass
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will pass (with the condition that the website is up and running) . toBeOK() is a method that ensures the response status code is within&nbsp;`200..299`&nbsp;range

        

## 5. The special word

Given the element has the text "Be the first to discover new tips and tricks about automation in software development" , in the code below, what do you think will happen 

`test("The innerText?", async ({ page }) =&gt; {
  await page.goto('https://blog.martioli.com');
  const innertText = page.locator(".gh-subscribe-description").innerText()
  await expect(innertText).toContain("Be the first to discover new tips")
});`Possible answers:

- Test will pass
- Test will fail with Error:&nbsp;expect Received&nbsp;object:&nbsp;{}
- Test will fail because we cannot use `toContain()` on `innerText()`
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will fail with Error:&nbsp;expect Received&nbsp;object:&nbsp;{} . Since we forgot the `await` key for the `innerText()` method to resolve the promise and extract the text.

        

## 6. The magic filter

What is the best and most recommended way to filter tests ?

            
                
#### Answer

                
                    
                        

                    
                
            
            Tags are the most simple and efficient way to filter your tests

        

## 7. The fail one

Given the code below and the fact that I am not an astronaut, what do you think will happen 

`test("The fail", async ({ page }) =&gt; {
  test.fail()
  await page.goto("https://www.martioli.com/");
  await expect(page.getByText('Astronaut')).toBeVisible()
});`Possible answers:

- Test will pass because of the `test.fail()` method applied
- Test will fail because of the `test.fail()` method applied
- Test will perform all the steps but still have a result as fail
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will pass because of the `test.fail()` method applied 

Why ? because it will not find the word "Astronaut" on my portofolio website and because it fails to find it, then our expectation of our test overall to fail is a success and the test will pass

        

## 8. The health check

Given the code below, what do you think will happen and how you can improve the code

`const locales = [
  "de",
  "com",
  "es"
]

for (const location of locales) {
  test(`check health: ${location}`, async ({ page }) =&gt; {
    const response = await page.request.get(`https://www.google.${location}/`)
    expect(response).toBeOK()
  });
}`Possible answers:

- Test will pass
- Test will fail because you cannot do such `for loops`
- Test will fail because expect has no `await` key
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will pass. You can do such checks in Playwright, just to be careful to put some delay inside the test before iteration goes to next item, to avoid ruining your test environments.

If you are wondering why it works expect without the await key see explanation at the end of the page...

        
 

## 9. The page one

Given the code below, what do you think will happen 

`test("The page one", async ({ page }) =&gt; {
  await page.goto("https://blog.martioli.com/");
  await expect(getByText('Recommended Resources')).toBeVisible()
});`Possible answers:

- Test will pass because we have `Recommended Resources`
- Test will fail because of Reference Error
- Test will fail because there is no `Recommended Resources` text on my blog
            
                
#### Answer

                
                    
                        

                    
                
            
            Test will fail with ReferenceError: getByRole is not defined . Notice that we have `expect(getByText` instead of `expect(page.getByText.`

        Become a member for free 
Hit the clap button if you found this useful. Or even buy me a coffee if you want to motivate me even more.

Playwright Interview Questions that you are going to hateEverybody hates it when they go to an interview and they are given live coding challenge that is written intentionally to make you fail, so the interviewer can prove his “superiority”. Here are 12 playwright interview questions and answers, so you can have the advantage this time.MartioliAdrian Maciuc
### Content below is visible to members only

My blog has a members section. Its free to join, there is no spam and you get access to members only content. Become a member now and you will also be the first to receive in your email when I publish a new article.

    
                
## This post is for subscribers only

            Subscribe now
            
Already have an account? Sign in
